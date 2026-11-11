package com.clinic.backend.identity;

import com.clinic.backend.identity.internal.application.IdentityApplicationService;
import org.springframework.stereotype.Component;
import com.clinic.backend.identity.internal.domain.IdentityException;

import java.util.Optional;
import java.util.UUID;
import java.util.Collection;
import java.util.List;
import java.util.Set;

/**
 * The only supported entry point for other business modules that need Identity data.
 * Keep this API small and persistence-agnostic.
 */
@Component
public class IdentityFacade {
    private final IdentityApplicationService service;

    public IdentityFacade(IdentityApplicationService service) {
        this.service = service;
    }

    public IdentityMembership createOwnerMembership(UUID ownerUserId, UUID clinicId) {
        try {
            return service.createOwnerMembership(ownerUserId, clinicId);
        } catch (IdentityException exception) {
            throw new IdentityFacadeException(exception.error().name(), exception.getMessage());
        }
    }

    public Optional<IdentityUser> findActiveUser(UUID userId) {
        return service.findActiveUser(userId);
    }

    public List<IdentityUser> findActiveUsers(Collection<UUID> userIds) {
        return service.findActiveUsers(userIds);
    }

    public Set<UUID> findActiveMembershipClinicIds(UUID userId) {
        return service.findActiveMembershipClinicIds(userId);
    }

    public Set<UUID> findActiveClinicIdsForRole(UUID userId, String role) {
        return service.findActiveClinicIdsForRole(userId, role);
    }

    public boolean hasActiveRole(UUID userId, UUID clinicId, String role) {
        return service.hasActiveRole(userId, clinicId, role);
    }

    public boolean hasAnyActiveRole(UUID userId, String role) {
        return service.hasAnyActiveRole(userId, role);
    }

    public boolean isActiveMember(UUID userId, UUID clinicId) {
        return service.isActiveMember(userId, clinicId);
    }

    public boolean hasPermission(UUID userId, UUID clinicId, String permission) {
        return service.hasPermission(userId, clinicId, permission);
    }

    public ProvisionedDoctorAccount provisionDoctorAccount(UUID actorUserId, UUID clinicId, String phone,
                                                            String fullName, String email, String password) {
        try {
            var result = service.provisionDoctorAccount(new com.clinic.backend.identity.internal.application.IdentityCommands.ProvisionDoctorAccount(
                    actorUserId, clinicId, phone, fullName, email, password));
            return new ProvisionedDoctorAccount(result.user(), result.membership());
        } catch (IdentityException exception) {
            throw new IdentityFacadeException(exception.error().name(), exception.getMessage());
        }
    }

    public ProvisionedDoctorAccount provisionDoctorAccountDirect(UUID actorUserId, UUID clinicId, String phone,
                                                            String fullName, String email, String temporaryPasswordHash) {
        try {
            var result = service.provisionDoctorAccountDirect(new com.clinic.backend.identity.internal.application.IdentityCommands.ProvisionDoctorAccountDirect(
                    actorUserId, clinicId, phone, fullName, email, temporaryPasswordHash));
            return new ProvisionedDoctorAccount(result.user(), result.membership());
        } catch (IdentityException exception) {
            throw new IdentityFacadeException(exception.error().name(), exception.getMessage());
        }
    }

    public record DoctorOnboardingInviteCreated(UUID inviteId, String phone, java.time.Instant expiresAt) {}
    public record DoctorInviteOutcome(boolean invited, UUID inviteId, String phone, java.time.Instant expiresAt,
                                      IdentityUser user, IdentityMembership membership) {}

    public DoctorOnboardingInviteCreated createDoctorOnboardingInvite(UUID actorUserId, UUID clinicId, String clinicName,
                                                                      String phone, String fullName, String email) {
        try {
            var r = service.createDoctorOnboardingInvite(new com.clinic.backend.identity.internal.application.IdentityCommands.CreateDoctorOnboardingInvite(
                    actorUserId, clinicId, clinicName, phone, fullName, email));
            return new DoctorOnboardingInviteCreated(r.inviteId(), r.phone(), r.expiresAt());
        } catch (IdentityException exception) {
            throw new IdentityFacadeException(exception.error().name(), exception.getMessage());
        }
    }

    public DoctorInviteOutcome inviteOrAddDoctor(UUID actorUserId, UUID clinicId, String clinicName, String phone,
                                                 String fullName, String email) {
        try {
            var r = service.createDoctorOnboardingInviteOrAdd(new com.clinic.backend.identity.internal.application.IdentityCommands.CreateDoctorOnboardingInvite(
                    actorUserId, clinicId, clinicName, phone, fullName, email));
            return new DoctorInviteOutcome(r.invited(), r.inviteId(), r.phone(), r.expiresAt(), r.user(), r.membership());
        } catch (IdentityException exception) {
            throw new IdentityFacadeException(exception.error().name(), exception.getMessage());
        }
    }

    public record ClaimedDoctorOnboarding(IdentityUser user, IdentityMembership membership, UUID clinicId) {}
    public ClaimedDoctorOnboarding claimDoctorOnboarding(String token, String password, String locale) {
        try {
            var r = service.claimDoctorOnboarding(new com.clinic.backend.identity.internal.application.IdentityCommands.ClaimDoctorOnboarding(token, password, locale, null));
            return new ClaimedDoctorOnboarding(r.user(), r.membership(), r.clinicId());
        } catch (IdentityException exception) {
            throw new IdentityFacadeException(exception.error().name(), exception.getMessage());
        }
    }

    public record IssuedTokens(String accessToken, java.time.Instant accessTokenExpiresAt, String refreshToken,
                               java.time.Instant refreshTokenExpiresAt, IdentityUser user, boolean mustChangePassword) {}
    public IssuedTokens issueTokens(UUID userId, String deviceId) {
        try {
            var t = service.issueTokens(userId, deviceId);
            return new IssuedTokens(t.accessToken(), t.accessTokenExpiresAt(), t.refreshToken(),
                    t.refreshTokenExpiresAt(), t.user(), t.mustChangePassword());
        } catch (IdentityException exception) {
            throw new IdentityFacadeException(exception.error().name(), exception.getMessage());
        }
    }

    public IssuedTokens changePassword(UUID userId, String currentPassword, String newPassword, String deviceId) {
        try {
            var t = service.changePassword(new com.clinic.backend.identity.internal.application.IdentityCommands.ChangePassword(userId, currentPassword, newPassword, deviceId));
            return new IssuedTokens(t.accessToken(), t.accessTokenExpiresAt(), t.refreshToken(),
                    t.refreshTokenExpiresAt(), t.user(), t.mustChangePassword());
        } catch (IdentityException exception) {
            throw new IdentityFacadeException(exception.error().name(), exception.getMessage());
        }
    }

    public boolean requiresPasswordChange(UUID userId) {
        return service.requiresPasswordChange(userId);
    }

    public boolean isSystemAdmin(UUID userId) {
        return service.isAdmin(userId);
    }

    public java.util.List<UUID> findSystemAdminUserIds() {
        return service.findSystemAdminUserIds();
    }
}
