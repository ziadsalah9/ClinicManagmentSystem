package com.clinic.backend.identity.internal.domain;

import java.time.Instant;
import java.util.UUID;

public record RefreshSession(
        UUID id,
        UUID userId,
        String tokenHash,
        Instant expiresAt,
        Instant revokedAt,
        UUID replacedBySessionId,
        String deviceId
) {
    public static RefreshSession create(UUID userId, String tokenHash,
                                        Instant expiresAt, String deviceId) {
        return new RefreshSession(UUID.randomUUID(), userId, tokenHash,
                expiresAt, null, null, deviceId);
    }

    public boolean isUsableAt(Instant now) {
        return revokedAt == null && expiresAt.isAfter(now);
    }

    public RefreshSession revoke(Instant now, UUID replacedBy) {
        return new RefreshSession(id, userId, tokenHash, expiresAt, now,
                replacedBy, deviceId);
    }
}
