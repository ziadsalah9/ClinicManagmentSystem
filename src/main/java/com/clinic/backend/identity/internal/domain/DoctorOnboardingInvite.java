package com.clinic.backend.identity.internal.domain;

import java.time.Instant;
import java.util.UUID;

/** Lightweight doctor onboarding invite: identity carries only contact/clinic context and a hashed claim token. */
public record DoctorOnboardingInvite(
        UUID id, UUID clinicId, String phone, String fullName, String email,
        String tokenHash, UUID invitedByUserId, DoctorOnboardingStatus status,
        int attemptCount, Instant expiresAt, Instant claimedAt, UUID claimedUserId
) {
    public static DoctorOnboardingInvite create(UUID clinicId, String phone, String fullName, String email,
                                                String tokenHash, UUID invitedByUserId, Instant expiresAt) {
        return new DoctorOnboardingInvite(UUID.randomUUID(), clinicId, phone, fullName, email, tokenHash, invitedByUserId,
                DoctorOnboardingStatus.PENDING, 0, expiresAt, null, null);
    }

    public boolean isClaimableAt(Instant now, int maxAttempts) {
        return status == DoctorOnboardingStatus.PENDING && expiresAt.isAfter(now) && attemptCount < maxAttempts;
    }

    public DoctorOnboardingInvite failedAttempt() {
        return new DoctorOnboardingInvite(id, clinicId, phone, fullName, email, tokenHash, invitedByUserId, status,
                attemptCount + 1, expiresAt, claimedAt, claimedUserId);
    }

    public DoctorOnboardingInvite claimed(Instant now, UUID userId) {
        return new DoctorOnboardingInvite(id, clinicId, phone, fullName, email, tokenHash, invitedByUserId,
                DoctorOnboardingStatus.CLAIMED, attemptCount, expiresAt, now, userId);
    }
}
