package com.clinic.backend.identity.internal.infrastructure.persistence;

import com.clinic.backend.identity.internal.application.port.IdentityStore;
import com.clinic.backend.identity.internal.domain.*;
import jakarta.persistence.EntityManager;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;
import java.util.Collection;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Repository
public class JpaIdentityStore implements IdentityStore {
    private final AppUserJpaRepository users;
    private final StaffMembershipJpaRepository memberships;
    private final InvitationJpaRepository invitations;
    private final OtpChallengeJpaRepository otpChallenges;
    private final RefreshSessionJpaRepository refreshSessions;
    private final EntityManager entityManager;
    private final PasswordLoginAttemptJpaRepository passwordLoginAttempts;
    private final DoctorOnboardingInviteJpaRepository doctorOnboardingInvites;

    JpaIdentityStore(AppUserJpaRepository users,
                     StaffMembershipJpaRepository memberships,
                     InvitationJpaRepository invitations,
                     OtpChallengeJpaRepository otpChallenges,
                     RefreshSessionJpaRepository refreshSessions,
                     PasswordLoginAttemptJpaRepository passwordLoginAttempts,
                     DoctorOnboardingInviteJpaRepository doctorOnboardingInvites,
                     EntityManager entityManager) {
        this.users = users;
        this.memberships = memberships;
        this.invitations = invitations;
        this.otpChallenges = otpChallenges;
        this.refreshSessions = refreshSessions;
        this.passwordLoginAttempts = passwordLoginAttempts;
        this.doctorOnboardingInvites = doctorOnboardingInvites;
        this.entityManager = entityManager;
    }

    @Override
    public Optional<AppUser> findUserById(UUID userId) {
        return users.findById(userId).map(JpaIdentityStore::toDomain);
    }

    @Override
    public Optional<AppUser> findUserByPhone(String phone) {
        return users.findByPhone(phone).map(JpaIdentityStore::toDomain);
    }

    @Override
    public List<AppUser> findUsersByIds(Collection<UUID> userIds) {
        return users.findAllById(userIds).stream().map(JpaIdentityStore::toDomain).toList();
    }

    @Override
    public AppUser saveUser(AppUser user) {
        Optional<AppUserJpaEntity> existing = users.findById(user.id());
        AppUserJpaEntity entity;
        if (existing.isPresent()) {
            entity = existing.get();
            entity.apply(user);
        } else {
            entity = new AppUserJpaEntity(user.id(), user.phone(), user.fullName(), user.email(),
                    user.passwordHash(), user.status(), user.locale(), user.systemRole(), user.mustChangePassword());
        }
        return toDomain(users.saveAndFlush(entity));
    }

    @Override
    @Transactional
    public void lockUserPhone(String phone) {
        entityManager.createNativeQuery("select pg_advisory_xact_lock(hashtextextended(:lockKey, 0))")
                .setParameter("lockKey", "user:" + phone)
                .getResultList();
    }

    @Override
    public Optional<PasswordLoginAttempt> findPasswordLoginAttemptForUpdate(String phone) {
        return passwordLoginAttempts.findByPhoneForUpdate(phone).map(entity ->
                new PasswordLoginAttempt(entity.getPhone(), entity.getFailedAttempts(),
                        entity.getWindowStartedAt(), entity.getLockedUntil()));
    }

    @Override
    public void savePasswordLoginAttempt(PasswordLoginAttempt attempt) {
        PasswordLoginAttemptJpaEntity entity = passwordLoginAttempts.findById(attempt.phone())
                .orElseGet(() -> new PasswordLoginAttemptJpaEntity(attempt.phone(), attempt.failedAttempts(),
                        attempt.windowStartedAt(), attempt.lockedUntil()));
        entity.apply(attempt);
        passwordLoginAttempts.saveAndFlush(entity);
    }

    @Override
    public void clearPasswordLoginAttempt(String phone) {
        passwordLoginAttempts.deleteById(phone);
    }

    @Override
    public Optional<StaffMembership> findMembership(UUID userId, UUID clinicId) {
        return memberships.findByUserIdAndClinicId(userId, clinicId).map(JpaIdentityStore::toDomain);
    }

    @Override
    public boolean existsMembership(UUID userId, UUID clinicId) {
        return memberships.existsByUserIdAndClinicId(userId, clinicId);
    }

    @Override
    public boolean hasActiveMembershipWithRole(UUID userId, UUID clinicId, StaffRole role) {
        return memberships.existsByUserIdAndClinicIdAndRoleAndStatus(userId, clinicId, role, MembershipStatus.ACTIVE);
    }

    @Override
    public boolean hasAnyActiveMembershipWithRole(UUID userId, StaffRole role) {
        return memberships.existsByUserIdAndRoleAndStatus(userId, role, MembershipStatus.ACTIVE);
    }

    @Override
    public Set<UUID> findActiveClinicIds(UUID userId) {
        return memberships.findAllByUserIdAndStatus(userId, MembershipStatus.ACTIVE).stream()
                .map(StaffMembershipJpaEntity::getClinicId).collect(Collectors.toUnmodifiableSet());
    }

    @Override
    public Set<UUID> findActiveClinicIds(UUID userId, StaffRole role) {
        return memberships.findAllByUserIdAndRoleAndStatus(userId, role, MembershipStatus.ACTIVE).stream()
                .map(StaffMembershipJpaEntity::getClinicId).collect(Collectors.toUnmodifiableSet());
    }

    @Override
    public StaffMembership saveMembership(StaffMembership membership) {
        StaffMembershipJpaEntity entity = new StaffMembershipJpaEntity(
                membership.id(), membership.userId(), membership.clinicId(), membership.role(),
                membership.status(), membership.permissions());
        return toDomain(memberships.saveAndFlush(entity));
    }

    @Override
    public void expirePendingInvitations(UUID clinicId, String phone, Instant now) {
        invitations.expirePending(clinicId, phone, now);
    }

    @Override
    public boolean existsPendingInvitation(UUID clinicId, String phone, Instant now) {
        return invitations.existsByClinicIdAndPhoneAndStatusAndExpiresAtAfter(
                clinicId, phone, InvitationStatus.PENDING, now);
    }

    @Override
    public Optional<Invitation> findInvitationForUpdate(String tokenHash) {
        return invitations.findByTokenHashForUpdate(tokenHash).map(JpaIdentityStore::toDomain);
    }

    @Override
    public Invitation saveInvitation(Invitation invitation) {
        InvitationJpaEntity entity = invitations.findById(invitation.id()).orElseGet(() -> new InvitationJpaEntity(
                invitation.id(), invitation.clinicId(), invitation.phone(), invitation.role(),
                invitation.permissions(), invitation.tokenHash(), invitation.invitedByUserId(),
                invitation.status(), invitation.expiresAt(), invitation.acceptedAt()));
        entity.apply(invitation);
        return toDomain(invitations.saveAndFlush(entity));
    }

    @Override
    @Transactional
    public void lockOtpRateLimitKey(String phone) {
        // A transaction-scoped advisory lock prevents simultaneous requests from bypassing the limit.
        entityManager.createNativeQuery("select pg_advisory_xact_lock(hashtextextended(:phone, 0))")
                .setParameter("phone", phone)
                .getResultList();
    }

    @Override
    public long countOtpRequestsSince(String phone, Instant since) {
        return otpChallenges.countByPhoneAndCreatedAtAfter(phone, since);
    }

    @Override
    public Optional<OtpChallenge> findOtpForUpdate(UUID challengeId) {
        return otpChallenges.findByIdForUpdate(challengeId).map(JpaIdentityStore::toDomain);
    }

    @Override
    public Optional<OtpChallenge> findVerifiedOtpForUpdate(String verificationTokenHash) {
        return otpChallenges.findByVerifiedTokenHashForUpdate(verificationTokenHash).map(JpaIdentityStore::toDomain);
    }

    @Override
    public OtpChallenge saveOtp(OtpChallenge challenge) {
        OtpChallengeJpaEntity entity = otpChallenges.findById(challenge.id()).orElseGet(() -> new OtpChallengeJpaEntity(
                challenge.id(), challenge.phone(), challenge.purpose(), challenge.codeHash(),
                challenge.attemptCount(), challenge.expiresAt(), challenge.verifiedTokenHash(),
                challenge.verifiedTokenExpiresAt(), challenge.consumedAt()));
        entity.apply(challenge);
        return toDomain(otpChallenges.saveAndFlush(entity));
    }

    @Override
    public Optional<RefreshSession> findActiveRefreshSessionForUpdate(String tokenHash) {
        return refreshSessions.findActiveByTokenHashForUpdate(tokenHash).map(JpaIdentityStore::toDomain);
    }

    @Override
    public RefreshSession saveRefreshSession(RefreshSession session) {
        RefreshSessionJpaEntity entity = refreshSessions.findById(session.id()).orElseGet(() -> new RefreshSessionJpaEntity(
                session.id(), session.userId(), session.tokenHash(), session.expiresAt(),
                session.revokedAt(), session.replacedBySessionId(), session.deviceId()));
        entity.apply(session);
        return toDomain(refreshSessions.saveAndFlush(entity));
    }

    @Override
    public List<UUID> findActiveSystemAdminUserIds() {
        return users.findIdsBySystemRoleAndStatus(com.clinic.backend.identity.internal.domain.SystemRole.ADMIN,
                com.clinic.backend.identity.internal.domain.UserStatus.ACTIVE);
    }

    @Override
    public long countDoctorOnboardingInvitesSince(String phone, Instant since) {
        return doctorOnboardingInvites.countByPhoneAndCreatedAtAfter(phone, since);
    }

    @Override
    public Optional<DoctorOnboardingInvite> findDoctorOnboardingInviteForUpdate(String tokenHash) {
        return doctorOnboardingInvites.findByTokenHashForUpdate(tokenHash).map(DoctorOnboardingInviteJpaEntity::toDomain);
    }

    @Override
    public DoctorOnboardingInvite saveDoctorOnboardingInvite(DoctorOnboardingInvite invite) {
        DoctorOnboardingInviteJpaEntity entity = doctorOnboardingInvites.findById(invite.id())
                .orElseGet(() -> new DoctorOnboardingInviteJpaEntity(invite));
        entity.apply(invite);
        return doctorOnboardingInvites.saveAndFlush(entity).toDomain();
    }

    private static AppUser toDomain(AppUserJpaEntity entity) {
        return new AppUser(entity.getId(), entity.getPhone(), entity.getFullName(), entity.getEmail(),
                entity.getPasswordHash(), entity.getStatus(), entity.getLocale(), entity.getSystemRole(),
                entity.isMustChangePassword());
    }

    private static StaffMembership toDomain(StaffMembershipJpaEntity entity) {
        return new StaffMembership(entity.getId(), entity.getUserId(), entity.getClinicId(),
                entity.getRole(), entity.getStatus(), entity.getPermissions());
    }

    private static Invitation toDomain(InvitationJpaEntity entity) {
        return new Invitation(entity.getId(), entity.getClinicId(), entity.getPhone(), entity.getRole(),
                entity.getPermissions(), entity.getTokenHash(), entity.getInvitedByUserId(),
                entity.getStatus(), entity.getExpiresAt(), entity.getAcceptedAt());
    }

    private static OtpChallenge toDomain(OtpChallengeJpaEntity entity) {
        return new OtpChallenge(entity.getId(), entity.getPhone(), entity.getPurpose(), entity.getCodeHash(),
                entity.getAttemptCount(), entity.getExpiresAt(), entity.getVerifiedTokenHash(),
                entity.getVerifiedTokenExpiresAt(), entity.getConsumedAt());
    }

    private static RefreshSession toDomain(RefreshSessionJpaEntity entity) {
        return new RefreshSession(entity.getId(), entity.getUserId(), entity.getTokenHash(),
                entity.getExpiresAt(), entity.getRevokedAt(), entity.getReplacedBySessionId(), entity.getDeviceId());
    }
}
