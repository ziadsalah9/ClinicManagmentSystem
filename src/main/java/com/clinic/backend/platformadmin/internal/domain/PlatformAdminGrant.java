package com.clinic.backend.platformadmin.internal.domain;

import java.time.Instant;
import java.util.Set;
import java.util.UUID;

public record PlatformAdminGrant(
        UUID id,
        UUID userId,
        GrantStatus status,
        Set<PlatformPermission> permissions,
        UUID grantedBy,
        UUID revokedBy,
        Instant revokedAt,
        long version
) {
    public PlatformAdminGrant {
        permissions = permissions == null ? Set.of() : Set.copyOf(permissions);
    }

    public static PlatformAdminGrant grant(UUID userId, Set<PlatformPermission> permissions, UUID grantedBy) {
        if (permissions == null || permissions.isEmpty()) {
            throw new IllegalArgumentException("At least one platform permission is required");
        }
        return new PlatformAdminGrant(UUID.randomUUID(), userId, GrantStatus.ACTIVE, permissions, grantedBy, null, null, 0);
    }

    public boolean isActive() { return status == GrantStatus.ACTIVE; }

    public boolean has(PlatformPermission permission) {
        if (!isActive()) return false;
        return permissions.stream().anyMatch(p -> p.satisfies(permission));
    }

    public boolean hasAny(Set<PlatformPermission> required) {
        if (!isActive() || required == null) return false;
        return required.stream().anyMatch(this::has);
    }

    public PlatformAdminGrant revoke(UUID actorUserId, Instant at) {
        return new PlatformAdminGrant(id, userId, GrantStatus.REVOKED, permissions, grantedBy, actorUserId, at, version);
    }
}
