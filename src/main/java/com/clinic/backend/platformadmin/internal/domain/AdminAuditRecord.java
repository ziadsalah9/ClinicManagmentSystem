package com.clinic.backend.platformadmin.internal.domain;

import java.time.Instant;
import java.util.UUID;

public record AdminAuditRecord(
        UUID id,
        UUID actorUserId,
        String actionType,
        String targetType,
        String targetId,
        String previousValue,
        String newValue,
        String reason,
        Instant createdAt
) {
    public static AdminAuditRecord create(UUID actorUserId, String actionType, String targetType, String targetId,
                                          String previousValue, String newValue, String reason) {
        return new AdminAuditRecord(UUID.randomUUID(), actorUserId, actionType, targetType, targetId,
                previousValue, newValue, reason, Instant.now());
    }
}
