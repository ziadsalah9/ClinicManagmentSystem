package com.clinic.backend.identity.internal.application;

import com.clinic.backend.identity.IdentityMembership;
import com.clinic.backend.identity.IdentityUser;

import java.time.Instant;
import java.util.UUID;

public final class IdentityResults {
    private IdentityResults() {
    }

    public record OtpRequested(UUID challengeId, Instant expiresAt) {
    }

    public record TokenPair(
            String accessToken,
            Instant accessTokenExpiresAt,
            String refreshToken,
            Instant refreshTokenExpiresAt,
            IdentityUser user,
            boolean mustChangePassword
    ) {
    }

    public record OtpVerification(
            String registrationVerificationToken,
            TokenPair tokenPair
    ) {
    }

    public record InvitationCreated(UUID invitationId, Instant expiresAt) {
    }

    public record AcceptedInvitation(IdentityUser user, IdentityMembership membership) {
    }

    public record ProvisionedDoctorAccount(IdentityUser user, IdentityMembership membership) {
    }

    public record DoctorOnboardingInviteCreated(UUID inviteId, String phone, Instant expiresAt) {
    }

    /**
     * Smart invite outcome: INVITED for a new user (claim token delivered), or ADDED_EXISTING when an
     * already-registered user is directly linked to the clinic as an active DOCTOR member (no token).
     */
    public record DoctorInviteOutcome(boolean invited, UUID inviteId, String phone, Instant expiresAt,
                                      IdentityUser user, IdentityMembership membership) {
        public static DoctorInviteOutcome invited(DoctorOnboardingInviteCreated invite) {
            return new DoctorInviteOutcome(true, invite.inviteId(), invite.phone(), invite.expiresAt(), null, null);
        }

        public static DoctorInviteOutcome addedExisting(String phone, IdentityUser user, IdentityMembership membership) {
            return new DoctorInviteOutcome(false, null, phone, null, user, membership);
        }
    }

    public record ClaimedDoctorOnboarding(IdentityUser user, IdentityMembership membership, TokenPair tokenPair,
                                          UUID clinicId) {
    }
}
