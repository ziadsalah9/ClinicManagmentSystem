package com.clinic.backend.doctor;

import com.clinic.backend.doctor.internal.application.DoctorApplicationService;
import org.springframework.stereotype.Component;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/** The only supported entry point for other modules that need doctor data. */
@Component
public class DoctorFacade {
    private final DoctorApplicationService service;

    public DoctorFacade(DoctorApplicationService service) {
        this.service = service;
    }

    public Optional<DoctorReference> findDoctor(UUID doctorId) {
        return service.findDoctorReference(doctorId);
    }

    public Optional<DoctorReference> findDoctorByUserId(UUID userId) {
        return service.findDoctorReferenceByUserId(userId);
    }

    public List<DoctorReference> findDoctors(Collection<UUID> doctorIds) {
        return service.findDoctorReferences(doctorIds);
    }

    public void assertActiveSpecialtiesExist(java.util.Set<UUID> specialtyIds) {
        service.assertActiveSpecialtiesExist(specialtyIds);
    }

    public DoctorInfo provisionDoctorProfile(UUID userId, String professionalTitle, String bio, int yearsExperience,
                                             String syndicateNumber, String gender, java.util.Set<UUID> specialtyIds) {
        try {
            com.clinic.backend.doctor.internal.domain.DoctorGender g = com.clinic.backend.doctor.internal.domain.DoctorGender.valueOf(gender);
            return service.provisionProfile(new com.clinic.backend.doctor.internal.application.DoctorCommands.SaveDoctorProfile(
                    userId, null, professionalTitle, bio, yearsExperience, syndicateNumber, g, specialtyIds));
        } catch (IllegalArgumentException e) {
            throw new com.clinic.backend.doctor.internal.domain.DoctorException(
                    com.clinic.backend.doctor.internal.domain.DoctorError.INVALID_INPUT, "Invalid gender");
        }
    }
}
