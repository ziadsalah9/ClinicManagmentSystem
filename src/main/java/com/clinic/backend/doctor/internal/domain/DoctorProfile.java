package com.clinic.backend.doctor.internal.domain;

import java.util.Set;
import java.util.UUID;

/** Pure doctor domain model. Account identity remains owned by Identity. */
public record DoctorProfile(
        UUID id,
        UUID userId,
        String professionalTitle,
        String bio,
        int yearsExperience,
        String syndicateNumber,
        DoctorGender gender,
        DoctorVerificationStatus verificationStatus,
        Set<UUID> specialtyIds,
        long version
) {
    public DoctorProfile {
        specialtyIds = specialtyIds == null ? Set.of() : Set.copyOf(specialtyIds);
    }

    public static DoctorProfile create(UUID userId, String professionalTitle, String bio,
                                        int yearsExperience, String syndicateNumber,
                                        DoctorGender gender, Set<UUID> specialtyIds) {
        return new DoctorProfile(UUID.randomUUID(), userId, professionalTitle, bio,
                yearsExperience, syndicateNumber, gender,
                DoctorVerificationStatus.PENDING_VERIFICATION, specialtyIds, 0);
    }

    public boolean matchesProfile(String professionalTitle, String bio, int yearsExperience,
                                  String syndicateNumber, DoctorGender gender, Set<UUID> specialtyIds) {
        return this.professionalTitle.equals(professionalTitle)
                && java.util.Objects.equals(this.bio, bio)
                && this.yearsExperience == yearsExperience
                && java.util.Objects.equals(this.syndicateNumber, syndicateNumber)
                && this.gender == gender
                && this.specialtyIds.equals(specialtyIds);
    }

    public DoctorProfile update(String professionalTitle, String bio, int yearsExperience,
                                 String syndicateNumber, DoctorGender gender,
                                 Set<UUID> specialtyIds) {
        // Any change to professional/public profile information requires re-verification.
        DoctorVerificationStatus nextStatus = verificationStatus == DoctorVerificationStatus.APPROVED
                ? DoctorVerificationStatus.PENDING_VERIFICATION : verificationStatus;
        return new DoctorProfile(id, userId, professionalTitle, bio, yearsExperience,
                syndicateNumber, gender, nextStatus, specialtyIds, version);
    }
}
