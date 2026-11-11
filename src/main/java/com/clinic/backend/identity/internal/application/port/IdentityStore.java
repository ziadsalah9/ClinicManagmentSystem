package com.clinic.backend.identity.internal.application.port;

import com.clinic.backend.identity.internal.domain.*;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;
import java.util.Collection;
import java.util.List;
import java.util.Set;

/** Application-owned abstraction implemented by the persistence layer. */
public interface IdentityStore {
    Optional<AppUser> findUserById(UUID userId);
    Optional<AppUser> findUserByPhone(String phone);
    List<AppUser> findUsersByIds(Collection<UUID> userIds);
    AppUser saveUser(AppUser user);
    void lockUserPhone(String phone);
    Optional<PasswordLoginAttempt> findPasswordLoginAttemptForUpdate(String phone);
    void savePasswordLoginAttempt(PasswordLoginAttempt attempt);
    void clearPasswordLoginAttempt(String phone);

    Optional<StaffMembership> findMembership(UUID userId, UUID clinicId);
    boolean existsMembership(UUID userId, UUID clinicId);
    boolean hasActiveMembershipWithRole(UUID userId, UUID clinicId, StaffRole role);
    boolean hasAnyActiveMembershipWithRole(UUID userId, StaffRole role);
    Set<UUID> findActiveClinicIds(UUID userId);
    Set<UUID> findActiveClinicIds(UUID userId, StaffRole role);
    StaffMembership saveMembership(StaffMembership membership);

    void expirePendingInvitations(UUID clinicId, String phone, Instant now);
    boolean existsPendingInvitation(UUID clinicId, String phone, Instant now);
    Optional<Invitation> findInvitationForUpdate(String tokenHash);
    Invitation saveInvitation(Invitation invitation);

    void lockOtpRateLimitKey(String phone);
    long countOtpRequestsSince(String phone, Instant since);
    Optional<OtpChallenge> findOtpForUpdate(UUID challengeId);
    Optional<OtpChallenge> findVerifiedOtpForUpdate(String verificationTokenHash);
    OtpChallenge saveOtp(OtpChallenge challenge);

    Optional<RefreshSession> findActiveRefreshSessionForUpdate(String tokenHash);
    RefreshSession saveRefreshSession(RefreshSession session);

    List<UUID> findActiveSystemAdminUserIds();

    long countDoctorOnboardingInvitesSince(String phone, Instant since);
    Optional<DoctorOnboardingInvite> findDoctorOnboardingInviteForUpdate(String tokenHash);
    DoctorOnboardingInvite saveDoctorOnboardingInvite(DoctorOnboardingInvite invite);
}
