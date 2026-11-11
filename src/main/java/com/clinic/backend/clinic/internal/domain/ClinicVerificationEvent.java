package com.clinic.backend.clinic.internal.domain;

import java.time.Instant;
import java.util.UUID;

public record ClinicVerificationEvent(
        UUID id,
        UUID clinicId,
        UUID actorUserId,
        String action,
        String previousStatus,
        String newStatus,
        String reason,
        Instant createdAt
) {
    public static ClinicVerificationEvent create(UUID clinicId, UUID actorUserId, String action,
                                                 String previousStatus, String newStatus,
                                                 String reason, Instant createdAt) {
        return new ClinicVerificationEvent(UUID.randomUUID(), clinicId, actorUserId, action,
                previousStatus, newStatus, reason, createdAt);
    }
}
