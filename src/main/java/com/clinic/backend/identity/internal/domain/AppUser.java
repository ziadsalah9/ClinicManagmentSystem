package com.clinic.backend.identity.internal.domain;

import java.util.UUID;

public record AppUser(
        UUID id,
        String phone,
        String fullName,
        String email,
        String passwordHash,
        UserStatus status,
        String locale,
        SystemRole systemRole,
        boolean mustChangePassword
) {
    public static AppUser registerOwner(String phone, String fullName, String email,
                                        String passwordHash, String locale) {
        return new AppUser(UUID.randomUUID(), phone, fullName, email, passwordHash,
                UserStatus.ACTIVE, locale == null ? "ar" : locale, SystemRole.USER, false);
    }

    public static AppUser acceptInvitation(String phone, String fullName,
                                           String passwordHash, String locale) {
        return new AppUser(UUID.randomUUID(), phone, fullName, null, passwordHash,
                UserStatus.ACTIVE, locale == null ? "ar" : locale, SystemRole.USER, false);
    }

    public static AppUser provisioned(String phone, String fullName, String email,
                                      String passwordHash, String locale, SystemRole systemRole) {
        return new AppUser(UUID.randomUUID(), phone, fullName, email, passwordHash,
                UserStatus.ACTIVE, locale == null ? "ar" : locale, systemRole == null ? SystemRole.USER : systemRole, false);
    }

    public static AppUser provisioned(String phone, String fullName, String email,
                                      String passwordHash, String locale, SystemRole systemRole, boolean mustChangePassword) {
        return new AppUser(UUID.randomUUID(), phone, fullName, email, passwordHash,
                UserStatus.ACTIVE, locale == null ? "ar" : locale, systemRole == null ? SystemRole.USER : systemRole, mustChangePassword);
    }

    public AppUser withPasswordHash(String newPasswordHash) {
        return new AppUser(id, phone, fullName, email, newPasswordHash, status, locale, systemRole, mustChangePassword);
    }

    public AppUser withPasswordChanged(String newPasswordHash) {
        return new AppUser(id, phone, fullName, email, newPasswordHash, status, locale, systemRole, false);
    }

    public boolean canAuthenticate() {
        return status == UserStatus.ACTIVE;
    }

    public boolean isAdmin() {
        return systemRole == SystemRole.ADMIN;
    }
}
