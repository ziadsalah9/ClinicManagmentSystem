package com.clinic.backend.identity.internal.domain;

import java.util.Set;
import java.util.UUID;

public record StaffMembership(
        UUID id,
        UUID userId,
        UUID clinicId,
        StaffRole role,
        MembershipStatus status,
        Set<String> permissions
) {
    public StaffMembership {
        permissions = permissions == null ? Set.of() : Set.copyOf(permissions);
    }

    public static StaffMembership owner(UUID userId, UUID clinicId) {
        return new StaffMembership(UUID.randomUUID(), userId, clinicId,
                StaffRole.OWNER, MembershipStatus.ACTIVE, Set.of());
    }

    public static StaffMembership invited(UUID userId, UUID clinicId,
                                           StaffRole role, Set<String> permissions) {
        return new StaffMembership(UUID.randomUUID(), userId, clinicId,
                role, MembershipStatus.ACTIVE, permissions);
    }

    public boolean isActive() {
        return status == MembershipStatus.ACTIVE;
    }

    public boolean hasPermission(String permission) {
        return role == StaffRole.OWNER || permissions.contains(permission);
    }
}
