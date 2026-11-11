package com.clinic.backend.provisioning.internal.presentation;

import com.clinic.backend.doctor.DoctorInfo;
import com.clinic.backend.identity.IdentityMembership;
import com.clinic.backend.identity.IdentityUser;
import jakarta.validation.constraints.*;

import java.time.Instant;
import java.util.Set;
import java.util.UUID;

public final class ProvisioningApiModels {
    private ProvisioningApiModels() {
    }

    /** Full doctor provisioning: creates identity + membership + doctor profile in one operation. */
    public record ProvisionDoctorRequest(
            @NotBlank @Pattern(regexp = "^\\+[1-9]\\d{7,14}$") String phone,
            @NotBlank @Size(max = 150) String fullName,
            @Email @Size(max = 254) String email,
            @NotBlank @Size(min = 10, max = 64) String password,
            @NotNull UUID clinicId,
            @NotBlank @Size(max = 120) String professionalTitle,
            @Size(max = 2000) String bio,
            @Min(0) @Max(80) int yearsExperience,
            @Size(max = 50) String syndicateNumber,
            @NotBlank @Pattern(regexp = "^(MALE|FEMALE|UNSPECIFIED)$") String gender,
            @NotNull @Size(min = 1, max = 8) Set<@NotNull UUID> specialtyIds
    ) {
    }

    /** Lightweight invite: only contact identity + target clinic. Full doctor profile is completed after claim. */
    public record InviteDoctorRequest(
            @NotBlank @Pattern(regexp = "^\\+[1-9]\\d{7,14}$") String phone,
            @NotBlank @Size(max = 150) String fullName,
            @Email @Size(max = 254) String email,
            @NotNull UUID clinicId
    ) {
    }

    public record DirectProvisionDoctorResponse(IdentityUser user, IdentityMembership membership, DoctorInfo doctor,
                                                String message, boolean mustChangePassword) {
    }

    public record InviteDoctorResponse(UUID inviteId, String phone, Instant expiresAt, String status,
                                        IdentityUser user, IdentityMembership membership, String message) {
        public static InviteDoctorResponse invited(UUID inviteId, String phone, Instant expiresAt) {
            return new InviteDoctorResponse(inviteId, phone, expiresAt, "INVITED", null, null,
                    "Lightweight doctor invitation created (phone/fullName/email/clinicId). Deliver the claim token through the configured secure channel; doctor completes profile after claim.");
        }

        public static InviteDoctorResponse addedExisting(String phone, IdentityUser user, IdentityMembership membership) {
            return new InviteDoctorResponse(null, phone, null, "ADDED_EXISTING", user, membership,
                    "Existing user is already registered; added as an active DOCTOR member of this clinic. No account creation or onboarding token was issued.");
        }
    }

    public record ProvisionDoctorResponse(IdentityUser user, IdentityMembership membership, DoctorInfo doctor, String message) {
    }

    public record ClaimDoctorRequest(
            @NotBlank String token,
            @NotBlank @Size(min = 10, max = 64) String password,
            @Pattern(regexp = "^(ar|en)$") String locale,
            @Size(max = 128) String deviceId
    ) {
    }

    public record TokenResponse(String accessToken, String tokenType, Instant accessTokenExpiresAt,
                                String refreshToken, Instant refreshTokenExpiresAt, IdentityUser user) {
    }

    public record ClaimDoctorResponse(IdentityUser user, IdentityMembership membership, TokenResponse tokens, String message) {
    }
}
