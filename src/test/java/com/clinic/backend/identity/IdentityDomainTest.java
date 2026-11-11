package com.clinic.backend.identity;

import com.clinic.backend.identity.internal.domain.*;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class IdentityDomainTest {

    @Test
    void ownerHasImplicitClinicPermissions() {
        StaffMembership owner = StaffMembership.owner(UUID.randomUUID(), UUID.randomUUID());

        assertThat(owner.hasPermission("STAFF_MANAGE")).isTrue();
        assertThat(owner.hasPermission("REFUND_APPROVE")).isTrue();
    }

    @Test
    void secretaryOnlyHasExplicitlyGrantedPermissions() {
        StaffMembership secretary = StaffMembership.invited(
                UUID.randomUUID(), UUID.randomUUID(), StaffRole.SECRETARY,
                Set.of("CHECK_IN", "PAYMENT_RECORD"));

        assertThat(secretary.hasPermission("CHECK_IN")).isTrue();
        assertThat(secretary.hasPermission("REFUND_APPROVE")).isFalse();
    }

    @Test
    void invitationCannotBeAcceptedTwice() {
        Instant now = Instant.parse("2026-11-03T12:00:00Z");
        Invitation pending = Invitation.create(UUID.randomUUID(), "+201012345678",
                StaffRole.DOCTOR, Set.of(), "hash", UUID.randomUUID(), now.plusSeconds(60));
        Invitation accepted = pending.accepted(now);

        assertThat(pending.isAcceptableAt(now)).isTrue();
        assertThat(accepted.isAcceptableAt(now)).isFalse();
    }

    @Test
    void passwordLoginAttemptsLockAfterFiveFailuresWithinTheWindow() {
        Instant now = Instant.parse("2026-11-03T12:00:00Z");
        PasswordLoginAttempt attempt = new PasswordLoginAttempt("+201012345678", 0, now, null);
        for (int index = 0; index < 5; index++) {
            attempt = attempt.failed(now.plusSeconds(index), 5,
                    java.time.Duration.ofMinutes(15), java.time.Duration.ofMinutes(15));
        }
        assertThat(attempt.failedAttempts()).isEqualTo(5);
        assertThat(attempt.isLockedAt(now.plusSeconds(5))).isTrue();
    }

    @Test
    void refreshSessionIsSingleUseAfterRevocation() {
        Instant now = Instant.now();
        RefreshSession session = RefreshSession.create(UUID.randomUUID(), "hash", now.plusSeconds(60), "device");

        assertThat(session.isUsableAt(now)).isTrue();
        assertThat(session.revoke(now, UUID.randomUUID()).isUsableAt(now)).isFalse();
    }
}
