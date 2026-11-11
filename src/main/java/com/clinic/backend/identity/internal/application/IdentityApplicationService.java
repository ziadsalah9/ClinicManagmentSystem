package com.clinic.backend.identity.internal.application;

import com.clinic.backend.identity.IdentityMembership;
import com.clinic.backend.identity.IdentityUser;
import com.clinic.backend.identity.internal.application.IdentityCommands.*;
import com.clinic.backend.identity.internal.application.IdentityResults.*;
import com.clinic.backend.identity.internal.application.port.AccessTokenIssuer;
import com.clinic.backend.identity.internal.application.port.DoctorInvitationEmailPort;
import com.clinic.backend.identity.internal.application.port.IdentityStore;
import com.clinic.backend.identity.internal.application.port.InvitationDeliveryPort;
import com.clinic.backend.identity.internal.application.port.OtpDeliveryPort;
import com.clinic.backend.identity.internal.domain.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.Base64;
import java.util.HexFormat;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.Collection;
import java.util.List;
import java.util.HashSet;
import java.util.regex.Pattern;

@Service
public class IdentityApplicationService {
    private static final Logger log = LoggerFactory.getLogger(IdentityApplicationService.class);
    private static final Pattern E164 = Pattern.compile("^\\+[1-9]\\d{7,14}$");
    private static final Set<String> ALLOWED_PERMISSIONS = Set.of(
            "BOOKINGS_MANAGE", "QUEUE_MANAGE", "CHECK_IN", "PAYMENT_RECORD",
            "REFUND_REQUEST", "REFUND_APPROVE", "DAY_CLOSE", "REPORTS_VIEW",
            "PRICES_MANAGE", "SCHEDULE_MANAGE", "STAFF_MANAGE",
            "PATIENT_FILE_MERGE", "PATIENT_MEDICAL_READ", "VISIT_WRITE",
            "CLINIC_PROFILE_UPDATE", "DOCTOR_MANAGE"
    );

    private final IdentityStore store;
    private final PasswordEncoder passwordEncoder;
    private final AccessTokenIssuer accessTokenIssuer;
    private final DoctorOnboardingAttemptRecorder attemptRecorder;
    private final OtpDeliveryPort otpDelivery;
    private final InvitationDeliveryPort invitationDelivery;
    private final DoctorInvitationEmailPort doctorInvitationEmail;
    private final Clock clock;
    private final SecureRandom secureRandom = new SecureRandom();

    private final Duration otpTtl;
    private final Duration verificationTokenTtl;
    private final Duration refreshTokenTtl;
    private final Duration otpRequestWindow;
    private final Duration invitationTtl;
    private final int maxOtpRequests;
    private final int maxOtpAttempts;
    private static final int MAX_PASSWORD_FAILURES = 5;
    private static final Duration PASSWORD_FAILURE_WINDOW = Duration.ofMinutes(15);
    private static final Duration PASSWORD_LOCK_DURATION = Duration.ofMinutes(15);

    public IdentityApplicationService(
            IdentityStore store,
            PasswordEncoder passwordEncoder,
            AccessTokenIssuer accessTokenIssuer,
            DoctorOnboardingAttemptRecorder attemptRecorder,
            OtpDeliveryPort otpDelivery,
            InvitationDeliveryPort invitationDelivery,
            DoctorInvitationEmailPort doctorInvitationEmail,
            Clock clock,
            @Value("${identity.otp.ttl:PT5M}") Duration otpTtl,
            @Value("${identity.otp.verification-token-ttl:PT5M}") Duration verificationTokenTtl,
            @Value("${security.jwt.refresh-token-ttl:P30D}") Duration refreshTokenTtl,
            @Value("${identity.otp.request-window:PT10M}") Duration otpRequestWindow,
            @Value("${identity.invitation.ttl:P3D}") Duration invitationTtl,
            @Value("${identity.otp.max-requests:3}") int maxOtpRequests,
            @Value("${identity.otp.max-attempts:5}") int maxOtpAttempts
    ) {
        this.store = store;
        this.passwordEncoder = passwordEncoder;
        this.accessTokenIssuer = accessTokenIssuer;
        this.attemptRecorder = attemptRecorder;
        this.otpDelivery = otpDelivery;
        this.invitationDelivery = invitationDelivery;
        this.doctorInvitationEmail = doctorInvitationEmail;
        this.clock = clock;
        this.otpTtl = otpTtl;
        this.verificationTokenTtl = verificationTokenTtl;
        this.refreshTokenTtl = refreshTokenTtl;
        this.otpRequestWindow = otpRequestWindow;
        this.invitationTtl = invitationTtl;
        this.maxOtpRequests = maxOtpRequests;
        this.maxOtpAttempts = maxOtpAttempts;
    }

    @Transactional
    public OtpRequested requestOtp(RequestOtp command) {
        String phone = normalizePhone(command.phone());
        if (command.purpose() == null) {
            throw error(IdentityError.INVALID_INPUT, "OTP purpose is required");
        }

        // Serializes concurrent OTP requests for the same phone at the PostgreSQL level.
        store.lockOtpRateLimitKey(phone);
        Instant now = clock.instant();
        long recentRequests = store.countOtpRequestsSince(phone, now.minus(otpRequestWindow));
        if (recentRequests >= maxOtpRequests) {
            throw error(IdentityError.OTP_RATE_LIMITED, "Too many OTP requests. Try again later.");
        }

        store.lockUserPhone(phone);
        Optional<AppUser> existingUser = store.findUserByPhone(phone);
        boolean eligible = command.purpose() == OtpPurpose.OWNER_REGISTRATION
                ? existingUser.isEmpty()
                : existingUser.filter(AppUser::canAuthenticate).isPresent();

        // Avoid account enumeration: persist a challenge for every request, but only deliver
        // a real code when the phone is eligible for the requested purpose.
        UUID challengeId = UUID.randomUUID();
        Instant expiresAt = now.plus(otpTtl);
        String code = generateOtp();
        OtpChallenge challenge = new OtpChallenge(challengeId, phone, command.purpose(),
                passwordEncoder.encode(code), 0, expiresAt, null, null, null);
        store.saveOtp(challenge);
        if (eligible) {
            UUID cid = challengeId;
            afterCommit(() -> otpDelivery.send(cid, phone, code));
        }
        return new OtpRequested(challengeId, expiresAt);
    }

    @Transactional(noRollbackFor = IdentityException.class)
    public OtpVerification verifyOtp(VerifyOtp command) {
        String phone = normalizePhone(command.phone());
        OtpChallenge challenge = store.findOtpForUpdate(command.challengeId())
                .orElseThrow(() -> error(IdentityError.OTP_INVALID, "Invalid OTP challenge"));
        Instant now = clock.instant();
        if (!challenge.phone().equals(phone)) {
            throw error(IdentityError.OTP_INVALID, "Invalid OTP challenge");
        }
        if (!challenge.expiresAt().isAfter(now)) {
            throw error(IdentityError.OTP_EXPIRED, "OTP has expired");
        }
        if (challenge.consumedAt() != null) {
            throw error(IdentityError.OTP_INVALID, "OTP has already been used");
        }
        if (challenge.attemptCount() >= maxOtpAttempts) {
            throw error(IdentityError.OTP_TOO_MANY_ATTEMPTS, "OTP attempt limit exceeded");
        }
        if (challenge.verifiedTokenHash() != null) {
            throw error(IdentityError.OTP_INVALID, "OTP has already been verified");
        }
        if (!passwordEncoder.matches(command.code(), challenge.codeHash())) {
            store.saveOtp(challenge.failedAttempt());
            throw error(IdentityError.OTP_INVALID, "Invalid OTP code");
        }

        if (challenge.purpose() == OtpPurpose.OWNER_REGISTRATION) {
            String rawVerificationToken = newOpaqueToken();
            OtpChallenge verified = challenge.verified(sha256(rawVerificationToken), now.plus(verificationTokenTtl));
            store.saveOtp(verified);
            return new OtpVerification(rawVerificationToken, null);
        }

        AppUser user = store.findUserByPhone(phone)
                .filter(AppUser::canAuthenticate)
                .orElseThrow(() -> error(IdentityError.INVALID_CREDENTIALS, "Invalid credentials"));
        store.saveOtp(challenge.consumed(now));
        return new OtpVerification(null, issueTokenPair(user, command.deviceId()));
    }

    @Transactional
    public IdentityUser registerOwner(RegisterOwner command) {
        String phone = normalizePhone(command.phone());
        validateFullName(command.fullName());
        validatePassword(command.password());
        store.lockUserPhone(phone);
        String verificationHash = sha256(command.verificationToken());
        OtpChallenge challenge = store.findVerifiedOtpForUpdate(verificationHash)
                .orElseThrow(() -> error(IdentityError.INVALID_VERIFICATION_TOKEN, "Invalid verification token"));
        Instant now = clock.instant();
        if (!challenge.phone().equals(phone) || challenge.purpose() != OtpPurpose.OWNER_REGISTRATION
                || !challenge.hasUsableVerificationTokenAt(now)) {
            throw error(IdentityError.INVALID_VERIFICATION_TOKEN, "Verification token is invalid or expired");
        }
        if (store.findUserByPhone(phone).isPresent()) {
            throw error(IdentityError.USER_ALREADY_EXISTS, "An account already exists for this phone");
        }
        String normalizedEmail = command.email() == null || command.email().isBlank()
                ? null : command.email().trim().toLowerCase(java.util.Locale.ROOT);
        AppUser user = AppUser.registerOwner(phone, command.fullName().trim(), normalizedEmail,
                passwordEncoder.encode(command.password()), "ar");
        try {
            store.saveUser(user);
        } catch (DataIntegrityViolationException exception) {
            throw error(IdentityError.USER_ALREADY_EXISTS, "An account already exists for this phone");
        }
        store.saveOtp(challenge.consumed(now));
        return toPublicUser(user);
    }

    @Transactional(noRollbackFor = IdentityException.class)
    public TokenPair passwordLogin(PasswordLogin command) {
        return doPasswordLogin(command.phone(), command.password(), command.deviceId(), null);
    }

    @Transactional(noRollbackFor = IdentityException.class)
    public TokenPair ownerPasswordLogin(OwnerPasswordLogin command) {
        return doPasswordLogin(command.phone(), command.password(), command.deviceId(), StaffRole.OWNER);
    }

    @Transactional(noRollbackFor = IdentityException.class)
    public TokenPair doctorPasswordLogin(DoctorPasswordLogin command) {
        return doPasswordLogin(command.phone(), command.password(), command.deviceId(), StaffRole.DOCTOR);
    }

    private TokenPair doPasswordLogin(String rawPhone, String password, String deviceId, StaffRole requiredRole) {
        String phone = normalizePhone(rawPhone);
        store.lockUserPhone(phone);
        Instant now = clock.instant();
        PasswordLoginAttempt attempt = store.findPasswordLoginAttemptForUpdate(phone).orElse(null);
        if (attempt != null && attempt.isLockedAt(now)) {
            throw error(IdentityError.AUTH_RATE_LIMITED, "Too many failed login attempts. Try again later.");
        }

        AppUser user = store.findUserByPhone(phone)
                .filter(AppUser::canAuthenticate)
                .orElse(null);
        boolean valid = user != null && user.passwordHash() != null
                && password != null && passwordEncoder.matches(password, user.passwordHash());
        if (!valid) {
            PasswordLoginAttempt next = (attempt == null
                    ? new PasswordLoginAttempt(phone, 0, now, null) : attempt)
                    .failed(now, MAX_PASSWORD_FAILURES, PASSWORD_FAILURE_WINDOW, PASSWORD_LOCK_DURATION);
            store.savePasswordLoginAttempt(next);
            if (next.isLockedAt(now)) {
                throw error(IdentityError.AUTH_RATE_LIMITED, "Too many failed login attempts. Try again later.");
            }
            throw error(IdentityError.INVALID_CREDENTIALS, "Invalid phone or password");
        }
        if (requiredRole != null && !store.hasAnyActiveMembershipWithRole(user.id(), requiredRole)) {
            throw error(IdentityError.PERMISSION_DENIED, "This account cannot use " + requiredRole.name().toLowerCase() + " password login");
        }
        store.clearPasswordLoginAttempt(phone);
        return issueTokenPair(user, deviceId);
    }

    @Transactional
    public TokenPair refresh(Refresh command) {
        String oldHash = sha256(command.refreshToken());
        RefreshSession oldSession = store.findActiveRefreshSessionForUpdate(oldHash)
                .orElseThrow(() -> error(IdentityError.INVALID_REFRESH_TOKEN, "Refresh token is invalid"));
        Instant now = clock.instant();
        if (!oldSession.isUsableAt(now)) {
            throw error(IdentityError.INVALID_REFRESH_TOKEN, "Refresh token is expired or revoked");
        }
        AppUser user = store.findUserById(oldSession.userId())
                .filter(AppUser::canAuthenticate)
                .orElseThrow(() -> error(IdentityError.USER_NOT_ACTIVE, "User is not active"));

        String rawRefresh = newOpaqueToken();
        RefreshSession replacement = RefreshSession.create(user.id(), sha256(rawRefresh),
                now.plus(refreshTokenTtl), command.deviceId() == null ? oldSession.deviceId() : command.deviceId());
        store.saveRefreshSession(replacement);
        store.saveRefreshSession(oldSession.revoke(now, replacement.id()));
        AccessTokenIssuer.IssuedAccessToken access = accessTokenIssuer.issue(user.id(), rolesFor(user), user.mustChangePassword());
        return new TokenPair(access.value(), access.expiresAt(), rawRefresh,
                replacement.expiresAt(), toPublicUser(user), user.mustChangePassword());
    }

    @Transactional
    public void logout(Logout command) {
        store.findActiveRefreshSessionForUpdate(sha256(command.refreshToken()))
                .ifPresent(session -> store.saveRefreshSession(session.revoke(clock.instant(), null)));
    }

    @Transactional
    public InvitationCreated createInvitation(CreateInvitation command) {
        UUID actorId = command.actorUserId();
        UUID clinicId = command.clinicId();
        if (!isActiveUser(actorId)) {
            throw error(IdentityError.PERMISSION_DENIED, "The account is inactive");
        }
        StaffMembership actorMembership = store.findMembership(actorId, clinicId)
                .filter(StaffMembership::isActive)
                .orElseThrow(() -> error(IdentityError.PERMISSION_DENIED, "You are not an active member of this clinic"));
        if (!actorMembership.hasPermission("STAFF_MANAGE")) {
            throw error(IdentityError.PERMISSION_DENIED, "You cannot invite staff in this clinic");
        }
        if (command.role() == null || command.role() == StaffRole.OWNER) {
            throw error(IdentityError.INVALID_ROLE, "This role cannot be invited");
        }
        String phone = normalizePhone(command.phone());
        Set<String> permissions = command.permissions() == null ? Set.of() : Set.copyOf(command.permissions());
        if (!ALLOWED_PERMISSIONS.containsAll(permissions)) {
            throw error(IdentityError.INVALID_PERMISSION, "One or more permissions are not supported");
        }
        if (store.findUserByPhone(phone).filter(user -> store.existsMembership(user.id(), clinicId)).isPresent()) {
            throw error(IdentityError.ALREADY_MEMBER, "This user is already a member of the clinic");
        }
        Instant now = clock.instant();
        store.expirePendingInvitations(clinicId, phone, now);
        if (store.existsPendingInvitation(clinicId, phone, now)) {
            throw error(IdentityError.ALREADY_MEMBER, "A pending invitation already exists for this phone");
        }
        String rawToken = newOpaqueToken();
        Invitation invitation = Invitation.create(clinicId, phone, command.role(), permissions,
                sha256(rawToken), actorId, now.plus(invitationTtl));
        try {
            store.saveInvitation(invitation);
        } catch (DataIntegrityViolationException exception) {
            throw error(IdentityError.ALREADY_MEMBER, "A pending invitation already exists for this phone");
        }
        afterCommit(() -> invitationDelivery.send(phone, rawToken, invitation.expiresAt()));
        return new InvitationCreated(invitation.id(), invitation.expiresAt());
    }

    @Transactional
    public AcceptedInvitation acceptInvitation(AcceptInvitation command) {
        if (command.token() == null || command.token().isBlank()) {
            throw error(IdentityError.INVALID_INVITATION, "Invitation token is required");
        }
        Invitation invitation = store.findInvitationForUpdate(sha256(command.token()))
                .orElseThrow(() -> error(IdentityError.INVALID_INVITATION, "Invitation is invalid"));
        Instant now = clock.instant();
        if (!invitation.isAcceptableAt(now)) {
            throw error(IdentityError.INVITATION_EXPIRED, "Invitation is expired or no longer active");
        }
        validateFullName(command.fullName());

        store.lockUserPhone(invitation.phone());
        AppUser user = store.findUserByPhone(invitation.phone()).orElse(null);
        if (user == null) {
            String passwordHash = null;
            if (command.password() != null && !command.password().isBlank()) {
                validatePassword(command.password());
                passwordHash = passwordEncoder.encode(command.password());
            } else if (invitation.role() != StaffRole.DOCTOR) {
                validatePassword(command.password());
            }
            user = AppUser.acceptInvitation(invitation.phone(), command.fullName().trim(),
                    passwordHash, command.locale());
            try {
                user = store.saveUser(user);
            } catch (DataIntegrityViolationException exception) {
                // A concurrent acceptance may have created the same account. Retry by phone on the next request.
                throw error(IdentityError.USER_ALREADY_EXISTS, "An account was created concurrently; retry invitation acceptance");
            }
        } else if (!user.canAuthenticate()) {
            throw error(IdentityError.USER_NOT_ACTIVE, "The invited account is not active");
        } else if (invitation.role() != StaffRole.DOCTOR && user.passwordHash() == null) {
            // A Doctor account may later be invited as a Secretary/Accountant in another clinic.
            // Add password authentication only when the existing account has none.
            validatePassword(command.password());
            user = store.saveUser(user.withPasswordHash(passwordEncoder.encode(command.password())));
        }

        if (store.existsMembership(user.id(), invitation.clinicId())) {
            throw error(IdentityError.ALREADY_MEMBER, "This user is already a member of the clinic");
        }
        StaffMembership membership = StaffMembership.invited(user.id(), invitation.clinicId(),
                invitation.role(), invitation.permissions());
        try {
            store.saveMembership(membership);
            store.saveInvitation(invitation.accepted(now));
        } catch (DataIntegrityViolationException exception) {
            throw error(IdentityError.ALREADY_MEMBER, "Membership already exists for this user and clinic");
        }
        return new AcceptedInvitation(toPublicUser(user), toPublicMembership(membership));
    }

    @Transactional
    public IdentityMembership createOwnerMembership(UUID ownerUserId, UUID clinicId) {
        AppUser owner = store.findUserById(ownerUserId)
                .filter(AppUser::canAuthenticate)
                .orElseThrow(() -> error(IdentityError.USER_NOT_FOUND, "Owner account was not found or is inactive"));
        if (store.existsMembership(ownerUserId, clinicId)) {
            throw error(IdentityError.ALREADY_MEMBER, "Owner already has a membership in this clinic");
        }
        StaffMembership membership = StaffMembership.owner(owner.id(), clinicId);
        try {
            store.saveMembership(membership);
        } catch (DataIntegrityViolationException exception) {
            throw error(IdentityError.ALREADY_MEMBER, "Owner already has a membership in this clinic");
        }
        return toPublicMembership(membership);
    }

    @Transactional
    public IdentityUser provisionOwner(ProvisionOwner command) {
        requireAdmin(command.actorUserId());
        return provisionAccount(command.phone(), command.fullName(), command.email(), command.password(), SystemRole.USER);
    }

    @Transactional
    public IdentityUser provisionAdmin(ProvisionAdmin command) {
        requireAdmin(command.actorUserId());
        return provisionAccount(command.phone(), command.fullName(), command.email(), command.password(), SystemRole.ADMIN);
    }

    private IdentityUser provisionAccount(String rawPhone, String fullName, String email, String password, SystemRole role) {
        String phone = normalizePhone(rawPhone);
        validateFullName(fullName);
        validatePassword(password);
        store.lockUserPhone(phone);
        if (store.findUserByPhone(phone).isPresent()) {
            throw error(IdentityError.USER_ALREADY_EXISTS, "An account already exists for this phone");
        }
        String normalizedEmail = email == null || email.isBlank()
                ? null : email.trim().toLowerCase(java.util.Locale.ROOT);
        AppUser user = AppUser.provisioned(phone, fullName.trim(), normalizedEmail,
                passwordEncoder.encode(password), "ar", role);
        try {
            store.saveUser(user);
        } catch (DataIntegrityViolationException exception) {
            throw error(IdentityError.USER_ALREADY_EXISTS, "An account already exists for this phone or email");
        }
        return toPublicUser(user);
    }

    @Transactional
    public IdentityResults.ProvisionedDoctorAccount provisionDoctorAccount(ProvisionDoctorAccount command) {
        return provisionDoctorAccountInternal(command.actorUserId(), command.clinicId(), command.phone(), command.fullName(),
                command.email(), passwordEncoder.encode(command.password()), false);
    }

    @Transactional
    public IdentityResults.ProvisionedDoctorAccount provisionDoctorAccountDirect(ProvisionDoctorAccountDirect command) {
        if (command.temporaryPasswordHash() == null || command.temporaryPasswordHash().isBlank()) {
            throw error(IdentityError.INVALID_INPUT, "Temporary password hash is required");
        }
        return provisionDoctorAccountInternal(command.actorUserId(), command.clinicId(), command.phone(), command.fullName(),
                command.email(), command.temporaryPasswordHash(), true);
    }

    private IdentityResults.ProvisionedDoctorAccount provisionDoctorAccountInternal(UUID actorUserId, UUID clinicId, String rawPhone,
            String fullName, String email, String encodedPassword, boolean mustChangePassword) {
        UUID actorId = requireActiveUserId(actorUserId);
        if (clinicId == null) throw error(IdentityError.INVALID_INPUT, "clinicId is required");
        boolean admin = isAdmin(actorId);
        if (!admin && !hasPermission(actorId, clinicId, "STAFF_MANAGE") && !hasActiveRole(actorId, clinicId, "OWNER")) {
            throw error(IdentityError.PERMISSION_DENIED, "Only an admin or clinic owner/staff manager can create doctor accounts");
        }
        String phone = normalizePhone(rawPhone);
        validateFullName(fullName);
        store.lockUserPhone(phone);
        if (store.findUserByPhone(phone).isPresent()) {
            throw error(IdentityError.USER_ALREADY_EXISTS, "An account already exists for this phone");
        }
        String normalizedEmail = email == null || email.isBlank() ? null : email.trim().toLowerCase(java.util.Locale.ROOT);
        AppUser user = AppUser.provisioned(phone, fullName.trim(), normalizedEmail, encodedPassword, "ar", SystemRole.USER, mustChangePassword);
        StaffMembership membership = StaffMembership.invited(user.id(), clinicId, StaffRole.DOCTOR, Set.of());
        try {
            store.saveUser(user);
            store.saveMembership(membership);
        } catch (DataIntegrityViolationException exception) {
            throw error(IdentityError.USER_ALREADY_EXISTS, "An account or membership already exists");
        }
        log.info("Provisioned doctor identity userId={} clinicId={} mustChangePassword={}", user.id(), clinicId, mustChangePassword);
        return new IdentityResults.ProvisionedDoctorAccount(toPublicUser(user), toPublicMembership(membership));
    }

    @Transactional
    public DoctorOnboardingInviteCreated createDoctorOnboardingInvite(CreateDoctorOnboardingInvite command) {
        DoctorInviteOutcome outcome = createDoctorOnboardingInviteOrAdd(command);
        if (!outcome.invited()) {
            throw error(IdentityError.ALREADY_MEMBER, "This user is already a member of the clinic");
        }
        return new DoctorOnboardingInviteCreated(outcome.inviteId(), outcome.phone(), outcome.expiresAt());
    }

    /**
     * Smart doctor invitation: if the phone is not registered, creates a lightweight claim-token invite.
     * If the phone already belongs to a registered user, links that existing user to the clinic as an
     * active DOCTOR (no account creation, no token) unless they are already an active DOCTOR/member of
     * that clinic, in which case ALREADY_MEMBER is raised.
     */
    @Transactional
    public DoctorInviteOutcome createDoctorOnboardingInviteOrAdd(CreateDoctorOnboardingInvite command) {
        UUID actorId = requireActiveUserId(command.actorUserId());
        UUID clinicId = command.clinicId();
        if (clinicId == null) throw error(IdentityError.INVALID_INPUT, "clinicId is required");
        boolean admin = isAdmin(actorId);
        if (!admin && !hasPermission(actorId, clinicId, "STAFF_MANAGE") && !hasActiveRole(actorId, clinicId, "OWNER")) {
            throw error(IdentityError.PERMISSION_DENIED, "Only an admin or clinic owner/staff manager can invite doctors");
        }
        String phone = normalizePhone(command.phone());
        validateFullName(command.fullName());
        store.lockOtpRateLimitKey(phone);
        Instant now = clock.instant();
        long recent = store.countDoctorOnboardingInvitesSince(phone, now.minus(Duration.ofHours(1)));
        if (recent >= 5) throw error(IdentityError.OTP_RATE_LIMITED, "Too many doctor onboarding invites for this phone. Try again later.");
        store.lockUserPhone(phone);

        Optional<AppUser> existingUser = store.findUserByPhone(phone);
        if (existingUser.isPresent()) {
            AppUser user = existingUser.get();
            // Schema enforces one membership per (clinic_id, user_id); reject any existing membership in that clinic.
            Optional<StaffMembership> existingMembership = store.findMembership(user.id(), clinicId);
            if (existingMembership.isPresent()) {
                StaffMembership m = existingMembership.get();
                if (m.isActive() && m.role() == StaffRole.DOCTOR) {
                    throw error(IdentityError.ALREADY_MEMBER, "This user is already an active doctor member of the clinic");
                }
                throw error(IdentityError.ALREADY_MEMBER, "This user is already a member of the clinic");
            }
            StaffMembership membership = StaffMembership.invited(user.id(), clinicId, StaffRole.DOCTOR, Set.of());
            try {
                store.saveMembership(membership);
            } catch (DataIntegrityViolationException exception) {
                throw error(IdentityError.ALREADY_MEMBER, "This user is already a member of the clinic");
            }
            log.info("Linked existing doctor userId={} to clinicId={} actorUserId={} (smart invite)", user.id(), clinicId, actorId);
            return DoctorInviteOutcome.addedExisting(phone, toPublicUser(user), toPublicMembership(membership));
        }

        String rawToken = newOpaqueToken();
        String invitedEmail = command.email() == null || command.email().isBlank()
                ? null : command.email().trim().toLowerCase(java.util.Locale.ROOT);
        String doctorName = command.fullName().trim();
        String clinicName = command.clinicName() == null || command.clinicName().isBlank()
                ? "the clinic" : command.clinicName().trim();
        DoctorOnboardingInvite invite = DoctorOnboardingInvite.create(clinicId, phone, doctorName, invitedEmail,
                sha256(rawToken), actorId, now.plus(invitationTtl));
        store.saveDoctorOnboardingInvite(invite);
        String ph = phone; Instant exp = invite.expiresAt();
        if (invitedEmail != null) {
            // The EmailService adapter is @Async; dispatching post-commit means a mail failure
            // can never roll back an invitation that has already been persisted.
            afterCommit(() -> {
                log.info("Dispatching doctor invitation email inviteId={} clinicId={}", invite.id(), clinicId);
                doctorInvitationEmail.sendDoctorInvitation(invitedEmail, doctorName, clinicName, rawToken);
            });
        } else {
            log.warn("Doctor onboarding invite inviteId={} clinicId={} has no email; using phone delivery",
                    invite.id(), clinicId);
            afterCommit(() -> invitationDelivery.send(ph, rawToken, exp));
        }
        log.info("Created doctor onboarding invite inviteId={} clinicId={} actorUserId={} emailDelivery={}",
                invite.id(), clinicId, actorId, invitedEmail != null);
        return DoctorInviteOutcome.invited(new DoctorOnboardingInviteCreated(invite.id(), phone, invite.expiresAt()));
    }

    @Transactional
    public ClaimedDoctorOnboarding claimDoctorOnboarding(ClaimDoctorOnboarding command) {
        if (command.token() == null || command.token().isBlank()) throw error(IdentityError.INVALID_INVITATION, "Onboarding token is required");
        validatePassword(command.password());
        Instant now = clock.instant();
        DoctorOnboardingInvite invite = store.findDoctorOnboardingInviteForUpdate(sha256(command.token()))
                .orElseThrow(() -> error(IdentityError.INVALID_INVITATION, "Onboarding token is invalid"));
        if (!invite.isClaimableAt(now, maxOtpAttempts)) {
            if (invite.attemptCount() >= maxOtpAttempts) {
                attemptRecorder.record(invite, store);
                throw error(IdentityError.OTP_TOO_MANY_ATTEMPTS, "Onboarding attempt limit exceeded");
            }
            attemptRecorder.record(invite.failedAttempt(), store);
            throw error(IdentityError.INVITATION_EXPIRED, "Onboarding invitation is expired or no longer active");
        }
        store.lockUserPhone(invite.phone());
        if (store.findUserByPhone(invite.phone()).isPresent()) throw error(IdentityError.USER_ALREADY_EXISTS, "An account already exists for this phone");
        AppUser user = AppUser.provisioned(invite.phone(), invite.fullName(), invite.email(),
                passwordEncoder.encode(command.password()), command.locale() == null ? "ar" : command.locale(), SystemRole.USER, false);
        StaffMembership membership = StaffMembership.invited(user.id(), invite.clinicId(), StaffRole.DOCTOR, Set.of());
        try {
            store.saveUser(user);
            store.saveMembership(membership);
            store.saveDoctorOnboardingInvite(invite.claimed(now, user.id()));
        } catch (DataIntegrityViolationException exception) {
            throw error(IdentityError.USER_ALREADY_EXISTS, "An account or membership already exists");
        }
        log.info("Claimed doctor onboarding identity inviteId={} userId={} clinicId={}", invite.id(), user.id(), invite.clinicId());
        return new ClaimedDoctorOnboarding(toPublicUser(user), toPublicMembership(membership), null, invite.clinicId());
    }

    @Transactional
    public TokenPair issueTokens(UUID userId, String deviceId) {
        AppUser user = store.findUserById(userId).filter(AppUser::canAuthenticate)
                .orElseThrow(() -> error(IdentityError.USER_NOT_FOUND, "User not found"));
        return issueTokenPair(user, deviceId);
    }

    @Transactional(readOnly = true)
    public boolean isAdmin(UUID userId) {
        return userId != null && store.findUserById(userId)
                .filter(AppUser::canAuthenticate)
                .map(AppUser::isAdmin)
                .orElse(false);
    }

    @Transactional(readOnly = true)
    public List<UUID> findSystemAdminUserIds() {
        return store.findActiveSystemAdminUserIds();
    }

    private void requireAdmin(UUID actorUserId) {
        if (!isAdmin(requireActiveUserId(actorUserId))) {
            throw error(IdentityError.PERMISSION_DENIED, "Admin role is required");
        }
    }

    private UUID requireActiveUserId(UUID actorUserId) {
        if (actorUserId == null || !isActiveUser(actorUserId)) {
            throw error(IdentityError.PERMISSION_DENIED, "Authentication is required");
        }
        return actorUserId;
    }

    @Transactional(readOnly = true)
    public List<IdentityUser> findActiveUsers(Collection<UUID> userIds) {
        if (userIds == null || userIds.isEmpty()) return List.of();
        return store.findUsersByIds(userIds).stream()
                .filter(AppUser::canAuthenticate)
                .map(this::toPublicUser)
                .toList();
    }

    @Transactional(readOnly = true)
    public Set<UUID> findActiveMembershipClinicIds(UUID userId) {
        return isActiveUser(userId) ? store.findActiveClinicIds(userId) : Set.of();
    }

    @Transactional(readOnly = true)
    public Set<UUID> findActiveClinicIdsForRole(UUID userId, String roleName) {
        StaffRole role = parseRole(roleName);
        return role == null || !isActiveUser(userId) ? Set.of() : store.findActiveClinicIds(userId, role);
    }

    @Transactional(readOnly = true)
    public boolean hasActiveRole(UUID userId, UUID clinicId, String roleName) {
        StaffRole role = parseRole(roleName);
        return role != null && isActiveUser(userId)
                && store.hasActiveMembershipWithRole(userId, clinicId, role);
    }

    @Transactional(readOnly = true)
    public boolean hasAnyActiveRole(UUID userId, String roleName) {
        StaffRole role = parseRole(roleName);
        return role != null && isActiveUser(userId)
                && store.hasAnyActiveMembershipWithRole(userId, role);
    }

    private StaffRole parseRole(String roleName) {
        if (roleName == null) return null;
        try { return StaffRole.valueOf(roleName); }
        catch (IllegalArgumentException exception) { return null; }
    }

    @Transactional
    public TokenPair changePassword(ChangePassword command) {
        if (command == null || command.userId() == null) throw error(IdentityError.INVALID_INPUT, "User is required");
        validatePassword(command.newPassword());
        AppUser user = store.findUserById(command.userId()).filter(AppUser::canAuthenticate)
                .orElseThrow(() -> error(IdentityError.USER_NOT_FOUND, "User not found"));
        store.lockUserPhone(user.phone());
        user = store.findUserById(command.userId()).filter(AppUser::canAuthenticate)
                .orElseThrow(() -> error(IdentityError.USER_NOT_FOUND, "User not found"));
        if (user.passwordHash() == null || command.currentPassword() == null || !passwordEncoder.matches(command.currentPassword(), user.passwordHash())) {
            throw error(IdentityError.INVALID_CREDENTIALS, "Current password is invalid");
        }
        if (passwordEncoder.matches(command.newPassword(), user.passwordHash())) {
            throw error(IdentityError.INVALID_INPUT, "New password must be different from the current password");
        }
        AppUser updated = store.saveUser(user.withPasswordChanged(passwordEncoder.encode(command.newPassword())));
        store.clearPasswordLoginAttempt(user.phone());
        return issueTokenPair(updated, command.deviceId());
    }

    @Transactional(readOnly = true)
    public boolean requiresPasswordChange(UUID userId) {
        return userId != null && store.findUserById(userId).map(AppUser::mustChangePassword).orElse(false);
    }

    @Transactional(readOnly = true)
    public Optional<IdentityUser> findActiveUser(UUID userId) {
        return store.findUserById(userId)
                .filter(AppUser::canAuthenticate)
                .map(this::toPublicUser);
    }

    @Transactional(readOnly = true)
    public boolean isActiveMember(UUID userId, UUID clinicId) {
        return isActiveUser(userId)
                && store.findMembership(userId, clinicId).filter(StaffMembership::isActive).isPresent();
    }

    @Transactional(readOnly = true)
    public boolean hasPermission(UUID userId, UUID clinicId, String permission) {
        return isActiveUser(userId) && store.findMembership(userId, clinicId)
                .filter(StaffMembership::isActive)
                .map(membership -> membership.hasPermission(permission))
                .orElse(false);
    }

    private boolean isActiveUser(UUID userId) {
        return userId != null && store.findUserById(userId).filter(AppUser::canAuthenticate).isPresent();
    }

    private TokenPair issueTokenPair(AppUser user, String deviceId) {
        Instant now = clock.instant();
        String rawRefresh = newOpaqueToken();
        RefreshSession refresh = RefreshSession.create(user.id(), sha256(rawRefresh),
                now.plus(refreshTokenTtl), deviceId);
        store.saveRefreshSession(refresh);
        AccessTokenIssuer.IssuedAccessToken access = accessTokenIssuer.issue(user.id(), rolesFor(user), user.mustChangePassword());
        return new TokenPair(access.value(), access.expiresAt(), rawRefresh,
                refresh.expiresAt(), toPublicUser(user), user.mustChangePassword());
    }

    private Set<String> rolesFor(AppUser user) {
        java.util.LinkedHashSet<String> roles = new java.util.LinkedHashSet<>();
        if (user.isAdmin()) roles.add("ROLE_ADMIN");
        if (store.hasAnyActiveMembershipWithRole(user.id(), StaffRole.OWNER)) roles.add("ROLE_OWNER");
        if (store.hasAnyActiveMembershipWithRole(user.id(), StaffRole.DOCTOR)) roles.add("ROLE_DOCTOR");
        return java.util.Collections.unmodifiableSet(roles);
    }

    private String normalizePhone(String phone) {
        if (phone == null || !E164.matcher(phone.trim()).matches()) {
            throw error(IdentityError.INVALID_PHONE, "Phone must be in E.164 format, for example +201012345678");
        }
        return phone.trim();
    }

    private void validateFullName(String fullName) {
        if (fullName == null || fullName.isBlank() || fullName.trim().length() > 150) {
            throw error(IdentityError.INVALID_INPUT, "Full name is required and must not exceed 150 characters");
        }
    }

    private void validatePassword(String password) {
        if (password == null || password.length() < 10 || password.length() > 64
                || password.getBytes(StandardCharsets.UTF_8).length > 72) {
            throw error(IdentityError.INVALID_INPUT, "Password must be 10-64 characters and at most 72 UTF-8 bytes");
        }
    }

    private String generateOtp() {
        return String.format("%06d", secureRandom.nextInt(1_000_000));
    }

    private String newOpaqueToken() {
        byte[] bytes = new byte[32];
        secureRandom.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }

    private String sha256(String raw) {
        if (raw == null || raw.isBlank()) {
            throw error(IdentityError.INVALID_INPUT, "Token is required");
        }
        try {
            byte[] digest = MessageDigest.getInstance("SHA-256").digest(raw.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(digest);
        } catch (Exception exception) {
            throw new IllegalStateException("SHA-256 is not available", exception);
        }
    }


    private void afterCommit(Runnable action) {
        if (!TransactionSynchronizationManager.isSynchronizationActive()) {
            throw new IllegalStateException("A transaction is required to schedule identity notifications");
        }
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCommit() {
                try {
                    action.run();
                } catch (RuntimeException exception) {
                    // Notification retries will be moved to the shared transactional outbox.
                    // The committed OTP/invitation remains valid and the caller can request a resend.
                    log.error("Identity notification delivery failed after commit", exception);
                }
            }
        });
    }

    private IdentityUser toPublicUser(AppUser user) {
        return new IdentityUser(user.id(), user.phone(), user.fullName(), user.status().name());
    }

    private IdentityMembership toPublicMembership(StaffMembership membership) {
        return new IdentityMembership(membership.id(), membership.userId(), membership.clinicId(),
                membership.role().name(), membership.status().name(), membership.permissions());
    }

    private IdentityException error(IdentityError error, String message) {
        return new IdentityException(error, message);
    }
}
