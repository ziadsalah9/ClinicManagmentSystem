package com.clinic.backend.identity;

import com.clinic.backend.identity.internal.application.DoctorOnboardingAttemptRecorder;
import com.clinic.backend.identity.internal.application.IdentityApplicationService;
import com.clinic.backend.identity.internal.application.IdentityCommands;
import com.clinic.backend.identity.internal.application.port.AccessTokenIssuer;
import com.clinic.backend.identity.internal.application.port.DoctorInvitationEmailPort;
import com.clinic.backend.identity.internal.application.port.IdentityStore;
import com.clinic.backend.identity.internal.application.port.InvitationDeliveryPort;
import com.clinic.backend.identity.internal.application.port.OtpDeliveryPort;
import com.clinic.backend.identity.internal.domain.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class IdentityApplicationServiceTest {
    @Mock IdentityStore store;
    @Mock PasswordEncoder passwordEncoder;
    @Mock AccessTokenIssuer accessTokenIssuer;
    @Mock DoctorOnboardingAttemptRecorder attemptRecorder;
    @Mock OtpDeliveryPort otpDelivery;
    @Mock InvitationDeliveryPort invitationDelivery;
    @Mock DoctorInvitationEmailPort doctorInvitationEmail;

    private IdentityApplicationService service;
    private final Clock clock = Clock.fixed(Instant.parse("2026-11-03T12:00:00Z"), ZoneOffset.UTC);

    @BeforeEach
    void setUp() {
        service = new IdentityApplicationService(
                store, passwordEncoder, accessTokenIssuer, attemptRecorder, otpDelivery, invitationDelivery,
                doctorInvitationEmail, clock,
                Duration.ofMinutes(5), Duration.ofMinutes(5), Duration.ofDays(30),
                Duration.ofMinutes(10), Duration.ofDays(3), 3, 5);
    }

    @Test
    void newDoctorInviteDispatchesEmailAfterCommit() {
        UUID actorId = UUID.randomUUID();
        UUID clinicId = UUID.randomUUID();
        String phone = "+201000000010";
        String email = "doctor@example.com";
        AppUser admin = new AppUser(actorId, "+201000000001", "Admin", null, "bcrypt-hash",
                UserStatus.ACTIVE, "en", SystemRole.ADMIN, false);

        when(store.findUserById(actorId)).thenReturn(Optional.of(admin));
        when(store.countDoctorOnboardingInvitesSince(eq(phone), any(Instant.class))).thenReturn(0L);
        when(store.findUserByPhone(phone)).thenReturn(Optional.empty());
        when(store.saveDoctorOnboardingInvite(any(DoctorOnboardingInvite.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        TransactionSynchronizationManager.initSynchronization();
        try {
            service.createDoctorOnboardingInviteOrAdd(new IdentityCommands.CreateDoctorOnboardingInvite(
                    actorId, clinicId, "Clinic", phone, "Dr Name", email));

            verifyNoInteractions(doctorInvitationEmail);

            TransactionSynchronizationManager.getSynchronizations()
                    .forEach(TransactionSynchronization::afterCommit);

            verify(doctorInvitationEmail)
                    .sendDoctorInvitation(eq(email), eq("Dr Name"), eq("Clinic"), anyString());
        } finally {
            TransactionSynchronizationManager.clearSynchronization();
        }
    }

    @Test
    void clinicCreationCanCreateAnOwnerMembershipThroughIdentityUseCase() {
        UUID userId = UUID.randomUUID();
        UUID clinicId = UUID.randomUUID();
        AppUser user = new AppUser(userId, "+201012345678", "Clinic Owner", null,
                "bcrypt-hash", UserStatus.ACTIVE, "ar", SystemRole.USER, false);
        when(store.findUserById(userId)).thenReturn(Optional.of(user));
        when(store.existsMembership(userId, clinicId)).thenReturn(false);
        when(store.saveMembership(any(StaffMembership.class))).thenAnswer(invocation -> invocation.getArgument(0));

        IdentityMembership result = service.createOwnerMembership(userId, clinicId);

        assertThat(result.userId()).isEqualTo(userId);
        assertThat(result.clinicId()).isEqualTo(clinicId);
        assertThat(result.role()).isEqualTo("OWNER");
        assertThat(result.status()).isEqualTo(MembershipStatus.ACTIVE.name());
        verify(store).saveMembership(argThat(membership -> membership.role().name().equals("OWNER")));
    }

//    @Test
//    void permissionCheckUsesMembershipScopeAndExplicitPermission() {
//        UUID userId = UUID.randomUUID();
//        UUID clinicId = UUID.randomUUID();
//        StaffMembership secretary = StaffMembership.invited(userId, clinicId,
//                com.clinic.backend.identity.internal.domain.StaffRole.SECRETARY,
//                Set.of("CHECK_IN"));
//        when(store.findMembership(userId, clinicId)).thenReturn(Optional.of(secretary));
//
//        assertThat(service.hasPermission(userId, clinicId, "CHECK_IN")).isTrue();
//        assertThat(service.hasPermission(userId, clinicId, "REFUND_APPROVE")).isFalse();
//        verify(store, times(2)).findMembership(userId, clinicId);
//    }
@Test
void permissionCheckUsesMembershipScopeAndExplicitPermission() {
    UUID userId = UUID.randomUUID();
    UUID clinicId = UUID.randomUUID();

    AppUser activeSecretary = new AppUser(
            userId,
            "+201012345678",
            "Secretary",
            null,
            "bcrypt-hash",
            UserStatus.ACTIVE,
            "ar",
            SystemRole.USER,
            false
    );

    when(store.findUserById(userId))
            .thenReturn(Optional.of(activeSecretary));

    StaffMembership secretary = StaffMembership.invited(
            userId,
            clinicId,
            StaffRole.SECRETARY,
            Set.of("CHECK_IN")
    );

    when(store.findMembership(userId, clinicId))
            .thenReturn(Optional.of(secretary));

    assertThat(service.hasPermission(userId, clinicId, "CHECK_IN"))
            .isTrue();

    assertThat(service.hasPermission(userId, clinicId, "REFUND_APPROVE"))
            .isFalse();

    verify(store, times(2)).findUserById(userId);
    verify(store, times(2)).findMembership(userId, clinicId);
}
}
