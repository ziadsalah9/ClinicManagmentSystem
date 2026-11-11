package com.clinic.backend.identity.internal.domain;

import java.time.Instant;
import java.util.Set;
import java.util.UUID;

public record Invitation(
        UUID id,
        UUID clinicId,
        String phone,
        StaffRole role,
        Set<String> permissions,
        String tokenHash,
        UUID invitedByUserId,
        InvitationStatus status,
        Instant expiresAt,
        Instant acceptedAt
) {
    public Invitation {
        permissions = permissions == null ? Set.of() : Set.copyOf(permissions);
    }

    public static Invitation create(UUID clinicId, String phone, StaffRole role,
                                    Set<String> permissions, String tokenHash,
                                    UUID invitedByUserId, Instant expiresAt) {
        return new Invitation(UUID.randomUUID(), clinicId, phone, role, permissions,
                tokenHash, invitedByUserId, InvitationStatus.PENDING, expiresAt, null);
    }

    public boolean isAcceptableAt(Instant now) {
        return status == InvitationStatus.PENDING && expiresAt.isAfter(now);
    }

    public Invitation accepted(Instant now) {
        return new Invitation(id, clinicId, phone, role, permissions, tokenHash,
                invitedByUserId, InvitationStatus.ACCEPTED, expiresAt, now);
    }
}
