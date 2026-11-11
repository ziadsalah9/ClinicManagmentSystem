package com.clinic.backend.doctor;

import java.util.Set;
import java.util.UUID;

/** Public doctor projection used by the API and other modules. */
public record DoctorInfo(
        UUID id,
        UUID userId,
        String fullName,
        String phone,
        String professionalTitle,
        String bio,
        int yearsExperience,
        String syndicateNumber,
        String gender,
        String verificationStatus,
        Set<UUID> specialtyIds,
        long version
) {
    public DoctorInfo {
        specialtyIds = specialtyIds == null ? Set.of() : Set.copyOf(specialtyIds);
    }
}
