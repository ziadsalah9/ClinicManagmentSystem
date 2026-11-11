package com.clinic.backend.doctor.internal.application;

import com.clinic.backend.doctor.internal.domain.DoctorGender;

import java.util.Set;
import java.util.UUID;

public final class DoctorCommands {
    private DoctorCommands() { }

    public record SaveDoctorProfile(UUID userId, Long expectedVersion, String professionalTitle, String bio,
                             int yearsExperience, String syndicateNumber,
                             DoctorGender gender, Set<UUID> specialtyIds) { }
}
