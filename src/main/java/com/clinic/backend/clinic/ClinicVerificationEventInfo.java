package com.clinic.backend.clinic;

import java.time.Instant;
import java.util.UUID;

/** Immutable verification audit entry exposed to the platform administration module. */
public record ClinicVerificationEventInfo(
        UUID id,
        UUID clinicId,
        UUID actorUserId,
        String action,
        String previousStatus,
        String newStatus,
        String reason,
        Instant createdAt
) { }
