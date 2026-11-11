package com.clinic.backend.provisioning;

import com.clinic.backend.clinic.ClinicFacade;
import com.clinic.backend.doctor.DoctorFacade;
import com.clinic.backend.identity.IdentityFacade;
import com.clinic.backend.provisioning.internal.application.ProvisioningApplicationService;
import com.clinic.backend.provisioning.internal.domain.ProvisioningError;
import com.clinic.backend.provisioning.internal.domain.ProvisioningException;
import com.clinic.backend.provisioning.internal.presentation.ProvisioningApiModels;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ProvisioningApplicationServiceTest {
    @Mock IdentityFacade identity;
    @Mock DoctorFacade doctors;
    @Mock ClinicFacade clinics;
    @Mock PasswordEncoder passwordEncoder;
    @InjectMocks ProvisioningApplicationService service;

    @Test
    void rejectsUnknownClinicBeforeWrites() {
        UUID clinicId = UUID.randomUUID();
        UUID actor = UUID.randomUUID();
        when(clinics.existsById(clinicId)).thenReturn(false);

        var req = new ProvisioningApiModels.ProvisionDoctorRequest("+201000000010", "D", "d@d.com", "AdminPass12345",
                clinicId, "Dr", "bio", 5, "S1", "MALE", Set.of(UUID.randomUUID()));

        assertThatThrownBy(() -> service.provisionDoctor(actor, req)).isInstanceOf(ProvisioningException.class)
                .extracting("error").isEqualTo(ProvisioningError.CLINIC_NOT_FOUND);
        verifyNoInteractions(identity);
        verify(doctors, never()).provisionDoctorProfile(any(), any(), any(), anyInt(), any(), any(), any());
    }

    @Test
    void rejectsOwnerProvisioningForUnownedClinic() {
        UUID clinicId = UUID.randomUUID();
        UUID ownerActor = UUID.randomUUID();
        when(clinics.existsById(clinicId)).thenReturn(true);
        when(identity.isSystemAdmin(ownerActor)).thenReturn(false);
        when(identity.hasAnyActiveRole(ownerActor, "OWNER")).thenReturn(true);
        when(clinics.isOwnedBy(clinicId, ownerActor)).thenReturn(false);

        var req = new ProvisioningApiModels.ProvisionDoctorRequest("+201000000010", "D", "d@d.com", "AdminPass12345",
                clinicId, "Dr", "bio", 5, "S1", "MALE", Set.of(UUID.randomUUID()));

        assertThatThrownBy(() -> service.provisionDoctor(ownerActor, req)).isInstanceOf(ProvisioningException.class)
                .extracting("error").isEqualTo(ProvisioningError.ACCESS_DENIED);
        verifyNoInteractions(doctors);
        verify(identity, never()).provisionDoctorAccount(any(), any(), any(), any(), any(), any());
    }

    @Test
    void createsLightweightInviteAfterClinicValidation() {
        UUID clinicId = UUID.randomUUID();
        UUID actor = UUID.randomUUID();
        when(clinics.existsById(clinicId)).thenReturn(true);
        when(clinics.findClinic(clinicId)).thenReturn(java.util.Optional.of(
                new com.clinic.backend.clinic.ClinicReference(clinicId, "Clinic", null, null, "UTC", "ACTIVE")));
        when(identity.isSystemAdmin(actor)).thenReturn(true);
        when(identity.inviteOrAddDoctor(actor, clinicId, "Clinic", "+201000000010", "Dr Name", "d@d.com"))
                .thenReturn(new IdentityFacade.DoctorInviteOutcome(true, UUID.randomUUID(), "+201000000010", null, null, null));

        var req = new ProvisioningApiModels.InviteDoctorRequest("+201000000010", "Dr Name", "d@d.com", clinicId);
        var outcome = service.inviteDoctor(actor, req);

        org.assertj.core.api.Assertions.assertThat(outcome.invited()).isTrue();
        verify(identity).inviteOrAddDoctor(actor, clinicId, "Clinic", "+201000000010", "Dr Name", "d@d.com");
        verifyNoInteractions(doctors);
    }

    @Test
    void linksExistingUserAsDoctorWhenAlreadyRegistered() {
        UUID clinicId = UUID.randomUUID();
        UUID actor = UUID.randomUUID();
        UUID existingUserId = UUID.randomUUID();
        when(clinics.existsById(clinicId)).thenReturn(true);
        when(clinics.findClinic(clinicId)).thenReturn(java.util.Optional.of(
                new com.clinic.backend.clinic.ClinicReference(clinicId, "Clinic", null, null, "UTC", "ACTIVE")));
        when(identity.isSystemAdmin(actor)).thenReturn(true);
        var user = new com.clinic.backend.identity.IdentityUser(existingUserId, "+201000000010", "Dr Name", "ACTIVE");
        var membership = new com.clinic.backend.identity.IdentityMembership(UUID.randomUUID(), existingUserId, clinicId, "DOCTOR", "ACTIVE", Set.of());
        when(identity.inviteOrAddDoctor(actor, clinicId, "Clinic", "+201000000010", "Dr Name", "d@d.com"))
                .thenReturn(new IdentityFacade.DoctorInviteOutcome(false, null, "+201000000010", null, user, membership));

        var req = new ProvisioningApiModels.InviteDoctorRequest("+201000000010", "Dr Name", "d@d.com", clinicId);
        var outcome = service.inviteDoctor(actor, req);

        org.assertj.core.api.Assertions.assertThat(outcome.invited()).isFalse();
        org.assertj.core.api.Assertions.assertThat(outcome.user().id()).isEqualTo(existingUserId);
        org.assertj.core.api.Assertions.assertThat(outcome.membership().role()).isEqualTo("DOCTOR");
        verify(identity).inviteOrAddDoctor(actor, clinicId, "Clinic", "+201000000010", "Dr Name", "d@d.com");
        verifyNoInteractions(doctors);
    }
}
