package com.clinic.backend.identity.internal.presentation;

import com.clinic.backend.identity.internal.application.IdentityApplicationService;
import com.clinic.backend.identity.internal.application.IdentityCommands;
import com.clinic.backend.identity.internal.application.IdentityResults;
import com.clinic.backend.identity.internal.infrastructure.notification.LocalOtpStore;
import com.clinic.backend.identity.internal.presentation.IdentityApiModels.*;
import io.swagger.v3.oas.annotations.Hidden;
import jakarta.validation.Valid;
import org.springframework.context.annotation.Profile;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

import java.util.Optional;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/auth")
public class AuthController {
    private final IdentityApplicationService identity;
    private final Optional<LocalOtpStore> localOtpStore;

    public AuthController(IdentityApplicationService identity, Optional<LocalOtpStore> localOtpStore) {
        this.identity = identity;
        this.localOtpStore = localOtpStore;
    }

    @Hidden
    @PostMapping("/otp/request")
    public OtpRequestedResponse requestOtp(@Valid @RequestBody RequestOtpRequest request) {
        IdentityResults.OtpRequested result = identity.requestOtp(
                new IdentityCommands.RequestOtp(request.phone(), request.purpose()));
        return new OtpRequestedResponse(result.challengeId(), result.expiresAt(),
                "If the phone is eligible, a verification code has been sent.");
    }

    @Hidden
    @PostMapping("/otp/verify")
    public VerifyOtpResponse verifyOtp(@Valid @RequestBody VerifyOtpRequest request) {
        IdentityResults.OtpVerification result = identity.verifyOtp(
                new IdentityCommands.VerifyOtp(request.phone(), request.challengeId(), request.code(), request.deviceId()));
        TokenResponse tokens = result.tokenPair() == null ? null : TokenResponse.from(result.tokenPair());
        return new VerifyOtpResponse(result.registrationVerificationToken(), tokens);
    }

    @PostMapping("/owner-registration")
    @ResponseStatus(HttpStatus.CREATED)
    public RegisterOwnerResponse registerOwner(@Valid @RequestBody RegisterOwnerRequest request) {
        var user = identity.registerOwner(new IdentityCommands.RegisterOwner(
                request.phone(), request.fullName(), request.email(), request.password(), request.verificationToken()));
        return new RegisterOwnerResponse(user, "Create your first clinic to activate the Owner membership.");
    }

    @PostMapping("/password/login")
    public TokenResponse passwordLogin(@Valid @RequestBody PasswordLoginRequest request) {
        return TokenResponse.from(identity.passwordLogin(
                new IdentityCommands.PasswordLogin(request.phone(), request.password(), request.deviceId())));
    }

    @Hidden
    @PostMapping("/owner/password-login")
    public TokenResponse ownerPasswordLogin(@Valid @RequestBody OwnerPasswordLoginRequest request) {
        return TokenResponse.from(identity.ownerPasswordLogin(
                new IdentityCommands.OwnerPasswordLogin(request.phone(), request.password(), request.deviceId())));
    }

    @Hidden
    @PostMapping("/doctor/password-login")
    public TokenResponse doctorPasswordLogin(@Valid @RequestBody DoctorPasswordLoginRequest request) {
        return TokenResponse.from(identity.doctorPasswordLogin(
                new IdentityCommands.DoctorPasswordLogin(request.phone(), request.password(), request.deviceId())));
    }

    @PostMapping("/refresh")
    public TokenResponse refresh(@Valid @RequestBody RefreshRequest request) {
        return TokenResponse.from(identity.refresh(new IdentityCommands.Refresh(request.refreshToken(), request.deviceId())));
    }

    @PostMapping("/logout")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void logout(@Valid @RequestBody LogoutRequest request) {
        identity.logout(new IdentityCommands.Logout(request.refreshToken()));
    }

    @PostMapping({"/password/change", "/change-password"})
    public TokenResponse changePassword(@AuthenticationPrincipal Jwt jwt, @Valid @RequestBody ChangePasswordRequest request) {
        var pair = identity.changePassword(new IdentityCommands.ChangePassword(
                UUID.fromString(jwt.getSubject()), request.currentPassword(), request.newPassword(), request.deviceId()));
        return TokenResponse.from(pair);
    }

    @PostMapping("/invitations/accept")
    @ResponseStatus(HttpStatus.CREATED)
    public AcceptInvitationResponse acceptInvitation(@Valid @RequestBody AcceptInvitationRequest request) {
        var result = identity.acceptInvitation(new IdentityCommands.AcceptInvitation(
                request.token(), request.fullName(), request.password(), request.locale()));
        return new AcceptInvitationResponse(result.user(), result.membership());
    }

    public record FakeOtpResponse(String phone, UUID challengeId, String code, String message) {
    }

    @Hidden
    @GetMapping("/otp/fake")
    @Profile("local")
    public FakeOtpResponse getFakeOtp(@RequestParam("phone") String phone) {
        LocalOtpStore store = localOtpStore.orElseThrow();
        LocalOtpStore.Entry entry = store.findLatestByPhone(phone)
                .orElseThrow(() -> new com.clinic.backend.identity.internal.domain.IdentityException(com.clinic.backend.identity.internal.domain.IdentityError.OTP_INVALID, "No OTP found for this phone in local memory"));
        return new FakeOtpResponse(entry.phone(), entry.challengeId(), entry.code(),
                "Dev only. Use with POST /api/v1/auth/otp/verify");
    }

    @Hidden
    @GetMapping("/otp/fake/{challengeId}")
    @Profile("local")
    public FakeOtpResponse getFakeOtpByChallenge(@PathVariable UUID challengeId) {
        LocalOtpStore store = localOtpStore.orElseThrow();
        LocalOtpStore.Entry entry = store.findByChallengeId(challengeId)
                .orElseThrow(() -> new com.clinic.backend.identity.internal.domain.IdentityException(com.clinic.backend.identity.internal.domain.IdentityError.OTP_INVALID, "No OTP found for this challengeId"));
        return new FakeOtpResponse(entry.phone(), entry.challengeId(), entry.code(),
                "Dev only. Use with POST /api/v1/auth/otp/verify");
    }
}
