package com.clinic.backend.platformadmin.internal.domain;

public enum PlatformPermission {
    PLATFORM_FULL_ADMIN,
    PLATFORM_CLINIC_VERIFICATION,
    PLATFORM_DOCTOR_VERIFICATION,
    PLATFORM_SUBSCRIPTION_ADMIN,
    PLATFORM_CATALOG_ADMIN,
    PLATFORM_REVIEW_MODERATOR;

    public boolean satisfies(PlatformPermission required) {
        return this == PLATFORM_FULL_ADMIN || this == required;
    }
}
