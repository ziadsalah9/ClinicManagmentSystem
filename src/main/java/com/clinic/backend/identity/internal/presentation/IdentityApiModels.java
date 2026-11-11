package com.clinic.backend.identity.internal.presentation;

import com.clinic.backend.identity.IdentityMembership;
import com.clinic.backend.identity.IdentityUser;
import com.clinic.backend.identity.internal.application.IdentityResults;
import com.clinic.backend.identity.internal.domain.OtpPurpose;
import com.clinic.backend.identity.internal.domain.StaffRole;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import java.time.Instant;
import java.util.Set;
import java.util.UUID;

public final class IdentityApiModels {
    private IdentityApiModels() {
    }

    public record RequestOtpRequest(
            @NotBlank @Pattern(regexp = "^\\+[1-9]\\d{7,14}$") String phone,
            @NotNull OtpPurpose purpose
    ) { }

    public record OtpRequestedResponse(UUID challengeId, Instant expiresAt, String message) { }

    public record VerifyOtpRequest(
            @NotBlank @Pattern(regexp = "^\\+[1-9]\\d{7,14}$") String phone,
            @NotNull UUID challengeId,
            @NotBlank @Pattern(regexp = "^\\d{6}$") String code,
            @Size(max = 128) String deviceId
    ) { }

    public record VerifyOtpResponse(String registrationVerificationToken, TokenResponse tokens) { }

    public record RegisterOwnerRequest(
            @NotBlank @Pattern(regexp = "^\\+[1-9]\\d{7,14}$") String phone,
            @NotBlank @Size(max = 150) String fullName,
            @Email @Size(max = 254) String email,
            @NotBlank @Size(min = 10, max = 64) String password,
            @NotBlank String verificationToken
    ) { }

    public record PasswordLoginRequest(
            @NotBlank @Pattern(regexp = "^\\+[1-9]\\d{7,14}$") String phone,
            @NotBlank @Size(max = 64) String password,
            @Size(max = 128) String deviceId
    ) { }

    public record RefreshRequest(@NotBlank String refreshToken, @Size(max = 128) String deviceId) { }
    public record LogoutRequest(@NotBlank String refreshToken) { }

    public record ChangePasswordRequest(@NotBlank @Size(max = 64) String currentPassword,
                                        @NotBlank @Size(min = 10, max = 64) String newPassword,
                                        @Size(max = 128) String deviceId) { }

    public record TokenResponse(
            String accessToken,
            String tokenType,
            Instant accessTokenExpiresAt,
            String refreshToken,
            Instant refreshTokenExpiresAt,
            IdentityUser user,
            boolean mustChangePassword
    ) {
        static TokenResponse from(IdentityResults.TokenPair pair) {
            return new TokenResponse(pair.accessToken(), "Bearer", pair.accessTokenExpiresAt(),
                    pair.refreshToken(), pair.refreshTokenExpiresAt(), pair.user(), pair.mustChangePassword());
        }
    }

    public record RegisterOwnerResponse(IdentityUser user, String nextStep) { }

    public record OwnerPasswordLoginRequest(
            @NotBlank @Pattern(regexp = "^\\+[1-9]\\d{7,14}$") String phone,
            @NotBlank @Size(max = 64) String password,
            @Size(max = 128) String deviceId
    ) { }

    public record DoctorPasswordLoginRequest(
            @NotBlank @Pattern(regexp = "^\\+[1-9]\\d{7,14}$") String phone,
            @NotBlank @Size(max = 64) String password,
            @Size(max = 128) String deviceId
    ) { }

    public record AcceptInvitationRequest(
            @NotBlank String token,
            @NotBlank @Size(max = 150) String fullName,
            @Size(min = 10, max = 64) String password,
            @Pattern(regexp = "^(ar|en)$") String locale
    ) { }

    public record AcceptInvitationResponse(IdentityUser user, IdentityMembership membership) { }

    public record CreateInvitationRequest(
            @NotBlank @Pattern(regexp = "^\\+[1-9]\\d{7,14}$") String phone,
            @NotNull StaffRole role,
            Set<@NotBlank @Size(max = 64) String> permissions
    ) { }

    public record CreateInvitationResponse(UUID invitationId, Instant expiresAt, String message) { }

    public record ProvisionOwnerRequest(
            @NotBlank @Pattern(regexp = "^\\+[1-9]\\d{7,14}$") String phone,
            @NotBlank @Size(max = 150) String fullName,
            @Email @Size(max = 254) String email,
            @NotBlank @Size(min = 10, max = 64) String password
    ) { }

    public record ProvisionOwnerResponse(IdentityUser user, String message) { }

    public record ProvisionAdminRequest(
            @NotBlank @Pattern(regexp = "^\\+[1-9]\\d{7,14}$") String phone,
            @NotBlank @Size(max = 150) String fullName,
            @Email @Size(max = 254) String email,
            @NotBlank @Size(min = 10, max = 64) String password
    ) { }

    public record ProvisionAdminResponse(IdentityUser user, String message) { }

    public record ProvisionDoctorRequest(
            @NotBlank @Pattern(regexp = "^\\+[1-9]\\d{7,14}$") String phone,
            @NotBlank @Size(max = 150) String fullName,
            @Email @Size(max = 254) String email,
            @NotBlank @Size(min = 10, max = 64) String password,
            @NotNull UUID clinicId,
            @NotBlank @Size(max = 120) String professionalTitle,
            @Size(max = 2000) String bio,
            @jakarta.validation.constraints.Min(0) @jakarta.validation.constraints.Max(80) int yearsExperience,
            @Size(max = 50) String syndicateNumber,
            @NotBlank @Pattern(regexp = "^(MALE|FEMALE|UNSPECIFIED)$") String gender,
            @NotNull @Size(min = 1, max = 8) Set<@NotNull UUID> specialtyIds
    ) { }

    public record ProvisionDoctorResponse(IdentityUser user, IdentityMembership membership, String message) { }
}
