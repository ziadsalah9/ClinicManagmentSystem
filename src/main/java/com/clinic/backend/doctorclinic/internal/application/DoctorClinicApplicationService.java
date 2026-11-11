package com.clinic.backend.doctorclinic.internal.application;

import com.clinic.backend.clinic.ClinicFacade;
import com.clinic.backend.clinic.ClinicReference;
import com.clinic.backend.doctor.DoctorFacade;
import com.clinic.backend.doctor.DoctorReference;
import com.clinic.backend.doctorclinic.DoctorClinicInfo;
import com.clinic.backend.doctorclinic.DoctorClinicPricing;
import com.clinic.backend.doctorclinic.DoctorClinicScheduleInfo;
import com.clinic.backend.doctorclinic.internal.domain.DoctorClinic;
import com.clinic.backend.doctorclinic.internal.domain.DoctorClinicError;
import com.clinic.backend.doctorclinic.internal.domain.DoctorClinicException;
import com.clinic.backend.identity.IdentityFacade;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
public class DoctorClinicApplicationService {
    private final DoctorClinicRepository associations;
    private final ClinicFacade clinics;
    private final DoctorFacade doctors;
    private final IdentityFacade identity;

    public DoctorClinicApplicationService(DoctorClinicRepository associations,
                                          ClinicFacade clinics,
                                          DoctorFacade doctors,
                                          IdentityFacade identity) {
        this.associations = associations;
        this.clinics = clinics;
        this.doctors = doctors;
        this.identity = identity;
    }

    @Transactional
    public DoctorClinicInfo updateSettings(DoctorClinicCommands.UpdateDoctorClinic command) {
        if (command.expectedVersion() != null && command.expectedVersion() < 0) {
            throw error(DoctorClinicError.INVALID_INPUT, "expectedVersion must be zero or greater");
        }
        ClinicReference clinic = clinics.findClinic(command.clinicId())
                .orElseThrow(() -> error(DoctorClinicError.CLINIC_NOT_FOUND, "Clinic was not found"));
        if ("SUSPENDED".equals(clinic.status()) || "REJECTED".equals(clinic.status())) {
            throw error(DoctorClinicError.CLINIC_NOT_AVAILABLE, "Doctors cannot be linked to a suspended or rejected clinic");
        }
        DoctorReference doctor = doctors.findDoctor(command.doctorId())
                .orElseThrow(() -> error(DoctorClinicError.DOCTOR_NOT_FOUND, "Doctor profile was not found"));
        // This operation changes fees, so a clinic staff member needs PRICES_MANAGE.
        // Owners pass this check because OWNER has all clinic permissions.
        boolean clinicManager = identity.hasPermission(command.actorUserId(), command.clinicId(), "PRICES_MANAGE");
        boolean targetDoctor = doctor.userId().equals(command.actorUserId())
                && identity.hasActiveRole(command.actorUserId(), command.clinicId(), "DOCTOR");
        if (!clinicManager && !targetDoctor) {
            throw error(DoctorClinicError.ACCESS_DENIED, "Only the clinic owner, a member with PRICES_MANAGE, or this doctor can change these settings");
        }
        if (!identity.hasActiveRole(doctor.userId(), command.clinicId(), "DOCTOR")) {
            throw error(DoctorClinicError.DOCTOR_NOT_MEMBER_OF_CLINIC,
                    "The doctor must accept an invitation to this clinic before being linked");
        }
        validateFees(command.consultationFee(), command.followUpFee(), command.followUpValidityDays());

        // Serialize concurrent create/update operations for the same clinic-doctor pair.
        associations.lockAssociation(command.clinicId(), command.doctorId());
        DoctorClinic current = associations.findByClinicAndDoctor(command.clinicId(), command.doctorId())
                .orElse(null);
        BigDecimal consultationFee = money(command.consultationFee());
        BigDecimal followUpFee = money(command.followUpFee());
        String instaPayAddress = cleanInstaPay(command.instaPayAddress());
        if (current != null && current.status() != com.clinic.backend.doctorclinic.internal.domain.DoctorClinicStatus.ACTIVE) {
            throw error(DoctorClinicError.CLINIC_NOT_AVAILABLE,
                    "This doctor-clinic relationship is not active");
        }
        if (current != null && current.matchesSettings(consultationFee, followUpFee,
                command.followUpValidityDays(), instaPayAddress, command.autoConfirm())) {
            return toInfo(current, clinic, doctor); // No-op update and retry do not bump the version.
        }
        if (current != null && (command.expectedVersion() == null
                || current.version() != command.expectedVersion())) {
            throw error(DoctorClinicError.STALE_VERSION,
                    "DoctorClinic settings changed. Reload the latest configuration before updating it.");
        }
        if (current == null && command.expectedVersion() != null) {
            throw error(DoctorClinicError.ASSOCIATION_NOT_FOUND,
                    "DoctorClinic configuration was not found for the supplied version");
        }
        DoctorClinic updated = current == null
                ? DoctorClinic.create(command.doctorId(), command.clinicId(), consultationFee, followUpFee,
                    command.followUpValidityDays(), instaPayAddress, command.autoConfirm())
                : current.updateSettings(consultationFee, followUpFee,
                    command.followUpValidityDays(), instaPayAddress, command.autoConfirm());
        try {
            DoctorClinic saved = associations.save(updated);
            return toInfo(saved, clinic, doctor);
        } catch (DataIntegrityViolationException exception) {
            throw error(DoctorClinicError.RELATIONSHIP_CONFLICT,
                    "A DoctorClinic configuration already exists or changed concurrently");
        }
    }

    @Transactional(readOnly = true)
    public java.util.Optional<DoctorClinicPricing> findActivePricing(UUID clinicId, UUID doctorId) {
        return associations.findByClinicAndDoctor(clinicId, doctorId)
                .filter(link -> link.status() == com.clinic.backend.doctorclinic.internal.domain.DoctorClinicStatus.ACTIVE)
                .map(link -> new DoctorClinicPricing(link.id(), link.clinicId(), link.doctorId(),
                        link.consultationFee(), link.followUpFee(), link.followUpValidityDays(),
                        link.instaPayAddress(), link.version()));
    }

    @Transactional(readOnly = true)
    public java.util.Optional<DoctorClinicScheduleInfo> findScheduleInfo(UUID doctorClinicId) {
        return associations.findById(doctorClinicId).flatMap(this::toScheduleInfo);
    }

    @Transactional(readOnly = true)
    public List<DoctorClinicScheduleInfo> findAllActiveScheduleInfo() {
        return associations.findAllActive().stream()
                .map(this::toScheduleInfo)
                .flatMap(java.util.Optional::stream)
                .toList();
    }

    private java.util.Optional<DoctorClinicScheduleInfo> toScheduleInfo(DoctorClinic link) {
        if (link.status() != com.clinic.backend.doctorclinic.internal.domain.DoctorClinicStatus.ACTIVE) {
            return java.util.Optional.empty();
        }
        return clinics.findClinic(link.clinicId())
                .map(clinic -> new DoctorClinicScheduleInfo(link.id(), link.clinicId(), link.doctorId(),
                        doctors.findDoctor(link.doctorId()).map(DoctorReference::userId).orElse(null),
                        clinic.timeZone(), link.status().name(), link.version()))
                .filter(info -> info.doctorUserId() != null);
    }

    @Transactional(readOnly = true)
    public List<DoctorClinicInfo> findForClinic(UUID clinicId, UUID actorUserId) {
        if (!identity.isActiveMember(actorUserId, clinicId)) {
            throw error(DoctorClinicError.ACCESS_DENIED, "You are not an active member of this clinic");
        }
        return buildForClinic(clinicId);
    }

    @Transactional(readOnly = true)
    public List<DoctorClinicInfo> findForDoctorUser(UUID doctorUserId) {
        DoctorReference doctor = doctors.findDoctorByUserId(doctorUserId)
                .orElseThrow(() -> error(DoctorClinicError.DOCTOR_NOT_FOUND, "Doctor profile was not found"));
        java.util.Set<UUID> activeDoctorClinicIds = identity.findActiveClinicIdsForRole(doctorUserId, "DOCTOR");
        List<DoctorClinic> links = associations.findByDoctorId(doctor.id()).stream()
                .filter(link -> activeDoctorClinicIds.contains(link.clinicId())).toList();
        if (links.isEmpty()) return List.of();
        List<UUID> clinicIds = links.stream().map(DoctorClinic::clinicId).distinct().toList();
        Map<UUID, ClinicReference> clinicById = clinics.findClinics(clinicIds).stream()
                .collect(Collectors.toMap(ClinicReference::id, Function.identity()));
        return links.stream().filter(link -> clinicById.containsKey(link.clinicId()))
                .map(link -> toInfo(link, clinicById.get(link.clinicId()), doctor)).toList();
    }

    private List<DoctorClinicInfo> buildForClinic(UUID clinicId) {
        ClinicReference clinic = clinics.findClinic(clinicId)
                .orElseThrow(() -> error(DoctorClinicError.CLINIC_NOT_FOUND, "Clinic was not found"));
        List<DoctorClinic> links = associations.findByClinicId(clinicId);
        if (links.isEmpty()) return List.of();
        Collection<UUID> doctorIds = links.stream().map(DoctorClinic::doctorId).distinct().toList();
        Map<UUID, DoctorReference> doctorById = doctors.findDoctors(doctorIds).stream()
                .collect(Collectors.toMap(DoctorReference::id, Function.identity()));
        return links.stream().filter(link -> doctorById.containsKey(link.doctorId()))
                .map(link -> toInfo(link, clinic, doctorById.get(link.doctorId()))).toList();
    }

    private static void validateFees(BigDecimal consultationFee, BigDecimal followUpFee, int validityDays) {
        if (consultationFee == null || followUpFee == null
                || consultationFee.signum() < 0 || followUpFee.signum() < 0) {
            throw error(DoctorClinicError.INVALID_INPUT, "Fees must be zero or greater");
        }
        try {
            consultationFee.setScale(2, RoundingMode.UNNECESSARY);
            followUpFee.setScale(2, RoundingMode.UNNECESSARY);
        } catch (ArithmeticException exception) {
            throw error(DoctorClinicError.INVALID_INPUT, "Fees support at most two decimal places");
        }
        if (consultationFee.precision() - consultationFee.scale() > 10
                || followUpFee.precision() - followUpFee.scale() > 10) {
            throw error(DoctorClinicError.INVALID_INPUT, "Fee exceeds supported amount");
        }
        if (validityDays < 1 || validityDays > 365) {
            throw error(DoctorClinicError.INVALID_INPUT, "Follow-up validity must be between 1 and 365 days");
        }
    }

    private static String cleanInstaPay(String address) {
        if (address == null || address.isBlank()) return null;
        String value = address.trim();
        if (value.length() > 150) throw error(DoctorClinicError.INVALID_INPUT, "InstaPay address must not exceed 150 characters");
        return value;
    }

    private static BigDecimal money(BigDecimal value) {
        return value.setScale(2, RoundingMode.UNNECESSARY);
    }

    private static DoctorClinicInfo toInfo(DoctorClinic link, ClinicReference clinic, DoctorReference doctor) {
        return new DoctorClinicInfo(link.id(), clinic, doctor, link.consultationFee(),
                link.followUpFee(), link.followUpValidityDays(), link.instaPayAddress(), link.autoConfirm(), link.status().name(), link.version());
    }

    private static DoctorClinicException error(DoctorClinicError error, String message) {
        return new DoctorClinicException(error, message);
    }
}
