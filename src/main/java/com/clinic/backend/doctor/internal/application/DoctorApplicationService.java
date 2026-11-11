package com.clinic.backend.doctor.internal.application;

import com.clinic.backend.doctor.DoctorInfo;
import com.clinic.backend.doctor.DoctorReference;
import com.clinic.backend.doctor.SpecialtyInfo;
import com.clinic.backend.doctor.internal.domain.DoctorError;
import com.clinic.backend.doctor.internal.domain.DoctorException;
import com.clinic.backend.doctor.internal.domain.DoctorProfile;
import com.clinic.backend.doctor.internal.domain.Specialty;
import com.clinic.backend.identity.IdentityFacade;
import com.clinic.backend.identity.IdentityUser;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
public class DoctorApplicationService {
    private final DoctorRepository doctors;
    private final IdentityFacade identity;

    public DoctorApplicationService(DoctorRepository doctors, IdentityFacade identity) {
        this.doctors = doctors;
        this.identity = identity;
    }

    @Transactional
    public DoctorInfo createMyProfile(DoctorCommands.SaveDoctorProfile command) {
        return createProfile(command, true);
    }

    @Transactional
    public DoctorInfo provisionProfile(DoctorCommands.SaveDoctorProfile command) {
        return createProfile(command, false);
    }

    private DoctorInfo createProfile(DoctorCommands.SaveDoctorProfile command, boolean requireDoctorMembership) {
        if (requireDoctorMembership) {
            ensureDoctorMembership(command.userId());
        }
        validate(command);
        doctors.lockProfile(command.userId());
        Set<UUID> requestedSpecialties = Set.copyOf(command.specialtyIds());
        String title = command.professionalTitle().trim();
        String bio = clean(command.bio());
        String syndicateNumber = clean(command.syndicateNumber());
        var existing = doctors.findByUserId(command.userId());
        if (existing.isPresent()) {
            DoctorProfile current = existing.orElseThrow();
            if (current.matchesProfile(title, bio, command.yearsExperience(), syndicateNumber,
                    command.gender(), requestedSpecialties)) {
                return toInfo(current, activeUser(command.userId()));
            }
            throw error(DoctorError.DOCTOR_PROFILE_ALREADY_EXISTS,
                    "A doctor profile already exists with different data for this account");
        }
        Set<UUID> specialties = validateSpecialties(requestedSpecialties);
        DoctorProfile profile = DoctorProfile.create(command.userId(), title, bio,
                command.yearsExperience(), syndicateNumber, command.gender(), specialties);
        try {
            DoctorProfile saved = doctors.save(profile);
            return toInfo(saved, activeUser(command.userId()));
        } catch (DataIntegrityViolationException exception) {
            throw error(DoctorError.DOCTOR_PROFILE_ALREADY_EXISTS,
                    "A doctor profile already exists for this account");
        }
    }

    @Transactional
    public DoctorInfo updateMyProfile(DoctorCommands.SaveDoctorProfile command) {
        ensureDoctorMembership(command.userId());
        validate(command);
        if (command.expectedVersion() == null || command.expectedVersion() < 0) {
            throw error(DoctorError.INVALID_INPUT, "expectedVersion is required when updating a doctor profile");
        }
        doctors.lockProfile(command.userId());
        DoctorProfile existing = doctors.findByUserId(command.userId())
                .orElseThrow(() -> error(DoctorError.DOCTOR_PROFILE_NOT_FOUND, "Doctor profile was not found"));
        Set<UUID> requestedSpecialties = Set.copyOf(command.specialtyIds());
        String title = command.professionalTitle().trim();
        String bio = clean(command.bio());
        String syndicateNumber = clean(command.syndicateNumber());
        if (existing.matchesProfile(title, bio, command.yearsExperience(), syndicateNumber,
                command.gender(), requestedSpecialties)) {
            return toInfo(existing, activeUser(command.userId())); // No-op update and replay do not bump the version.
        }
        if (existing.version() != command.expectedVersion()) {
            throw error(DoctorError.STALE_VERSION,
                    "Doctor profile changed. Reload the latest profile before updating it.");
        }
        Set<UUID> specialties = validateSpecialties(requestedSpecialties);
        DoctorProfile updated = existing.update(title, bio,
                command.yearsExperience(), syndicateNumber, command.gender(), specialties);
        try {
            return toInfo(doctors.save(updated), activeUser(command.userId()));
        } catch (DataIntegrityViolationException exception) {
            throw error(DoctorError.DOCTOR_PROFILE_ALREADY_EXISTS,
                    "The syndicate number is already assigned to another doctor");
        }
    }

    @Transactional(readOnly = true)
    public DoctorInfo getMyProfile(UUID userId) {
        DoctorProfile profile = doctors.findByUserId(userId)
                .orElseThrow(() -> error(DoctorError.DOCTOR_PROFILE_NOT_FOUND, "Doctor profile was not found"));
        return toInfo(profile, activeUser(userId));
    }

    @Transactional(readOnly = true)
    public List<SpecialtyInfo> getSpecialties() {
        return doctors.findActiveSpecialties().stream()
                .map(s -> new SpecialtyInfo(s.id(), s.code(), s.nameEn(), s.nameAr())).toList();
    }

    @Transactional(readOnly = true)
    public void assertActiveSpecialtiesExist(Set<UUID> specialtyIds) {
        if (specialtyIds == null || specialtyIds.isEmpty()) {
            throw error(DoctorError.INVALID_INPUT, "At least one specialty is required");
        }
        Set<UUID> ids = Set.copyOf(specialtyIds);
        Set<UUID> found = doctors.findActiveSpecialtyIds(ids);
        if (found.size() != ids.size()) {
            throw error(DoctorError.SPECIALTY_NOT_FOUND,
                    "One or more specialties do not exist or are inactive");
        }
    }

    @Transactional(readOnly = true)
    public java.util.Optional<DoctorReference> findDoctorReference(UUID doctorId) {
        return doctors.findById(doctorId).flatMap(profile -> identity.findActiveUser(profile.userId())
                .map(user -> toReference(profile, user)));
    }

    @Transactional(readOnly = true)
    public java.util.Optional<DoctorReference> findDoctorReferenceByUserId(UUID userId) {
        return doctors.findByUserId(userId).flatMap(profile -> identity.findActiveUser(userId)
                .map(user -> toReference(profile, user)));
    }



    @Transactional(readOnly = true)
    public List<DoctorReference> findDoctorReferences(Collection<UUID> doctorIds) {
        if (doctorIds == null || doctorIds.isEmpty()) return List.of();
        List<DoctorProfile> profiles = doctors.findByIds(doctorIds);
        Map<UUID, IdentityUser> users = identity.findActiveUsers(
                profiles.stream().map(DoctorProfile::userId).collect(Collectors.toSet()))
                .stream().collect(Collectors.toMap(IdentityUser::id, Function.identity()));
        return profiles.stream().filter(profile -> users.containsKey(profile.userId()))
                .map(profile -> toReference(profile, users.get(profile.userId()))).toList();
    }

    private void ensureDoctorMembership(UUID userId) {
        if (!identity.hasAnyActiveRole(userId, "DOCTOR")) {
            throw error(DoctorError.NOT_A_DOCTOR,
                    "An active Doctor membership is required before creating or changing this profile");
        }
    }

    private IdentityUser activeUser(UUID userId) {
        return identity.findActiveUser(userId)
                .orElseThrow(() -> error(DoctorError.ACCESS_DENIED, "User account is inactive"));
    }

    private Set<UUID> validateSpecialties(Set<UUID> requested) {
        Set<UUID> ids = requested == null ? Set.of() : Set.copyOf(requested);
        Set<UUID> found = doctors.findActiveSpecialtyIds(ids);
        if (found.size() != ids.size()) {
            throw error(DoctorError.SPECIALTY_NOT_FOUND,
                    "One or more specialties do not exist or are inactive");
        }
        return found;
    }

    private static void validate(DoctorCommands.SaveDoctorProfile command) {
        if (command.professionalTitle() == null || command.professionalTitle().isBlank()
                || command.professionalTitle().trim().length() > 120) {
            throw error(DoctorError.INVALID_INPUT,
                    "Professional title is required and must not exceed 120 characters");
        }
        if (command.bio() != null && command.bio().length() > 2000) {
            throw error(DoctorError.INVALID_INPUT, "Bio must not exceed 2000 characters");
        }
        if (command.yearsExperience() < 0 || command.yearsExperience() > 80) {
            throw error(DoctorError.INVALID_INPUT, "Years of experience must be between 0 and 80");
        }
        if (command.syndicateNumber() != null && command.syndicateNumber().length() > 50) {
            throw error(DoctorError.INVALID_INPUT, "Syndicate number must not exceed 50 characters");
        }
        if (command.gender() == null) throw error(DoctorError.INVALID_INPUT, "Gender is required");
        if (command.specialtyIds() == null || command.specialtyIds().isEmpty() || command.specialtyIds().size() > 8
                || command.specialtyIds().stream().anyMatch(java.util.Objects::isNull)) {
            throw error(DoctorError.INVALID_INPUT, "Select between 1 and 8 valid specialties");
        }
    }

    private static String clean(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }

    private static DoctorReference toReference(DoctorProfile profile, IdentityUser user) {
        return new DoctorReference(profile.id(), profile.userId(), user.fullName(),
                profile.professionalTitle(), profile.verificationStatus().name());
    }

    private static DoctorInfo toInfo(DoctorProfile profile, IdentityUser user) {
        return new DoctorInfo(profile.id(), profile.userId(), user.fullName(), user.phone(),
                profile.professionalTitle(), profile.bio(), profile.yearsExperience(),
                profile.syndicateNumber(), profile.gender().name(), profile.verificationStatus().name(),
                profile.specialtyIds(), profile.version());
    }

    private static DoctorException error(DoctorError error, String message) {
        return new DoctorException(error, message);
    }
}
