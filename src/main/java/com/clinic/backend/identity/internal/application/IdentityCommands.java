package com.clinic.backend.identity.internal.application;

import com.clinic.backend.identity.internal.domain.OtpPurpose;
import com.clinic.backend.identity.internal.domain.StaffRole;

import java.util.Set;
import java.util.UUID;

public final class IdentityCommands {
    private IdentityCommands() {
    }

    public record RequestOtp(String phone, OtpPurpose purpose) {
    }

    public record VerifyOtp(String phone, UUID challengeId, String code, String deviceId) {
    }

    public record RegisterOwner(String phone, String fullName, String email,
                                String password, String verificationToken) {
    }

    public record PasswordLogin(String phone, String password, String deviceId) {
    }

    public record OwnerPasswordLogin(String phone, String password, String deviceId) {
    }

    public record DoctorPasswordLogin(String phone, String password, String deviceId) {
    }

    public record Refresh(String refreshToken, String deviceId) {
    }

    public record Logout(String refreshToken) {
    }

    public record CreateInvitation(UUID actorUserId, UUID clinicId, String phone,
                                   StaffRole role, Set<String> permissions) {
    }

    public record AcceptInvitation(String token, String fullName, String password,
                                   String locale) {
    }

    public record ProvisionOwner(UUID actorUserId, String phone, String fullName, String email, String password) {
    }

    public record ProvisionAdmin(UUID actorUserId, String phone, String fullName, String email, String password) {
    }

    public record ProvisionDoctorAccount(UUID actorUserId, UUID clinicId, String phone, String fullName, String email,
                                         String password) {
    }

    public record ProvisionDoctorAccountDirect(UUID actorUserId, UUID clinicId, String phone, String fullName, String email,
                                               String temporaryPasswordHash) {
    }

    public record CreateDoctorOnboardingInvite(UUID actorUserId, UUID clinicId, String clinicName, String phone,
                                               String fullName, String email) {
    }

    public record ClaimDoctorOnboarding(String token, String password, String locale, String deviceId) {
    }

    public record ChangePassword(UUID userId, String currentPassword, String newPassword, String deviceId) {
    }
}
