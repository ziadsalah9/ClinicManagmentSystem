package com.clinic.backend.identity;

import java.util.Set;
import java.util.UUID;

/** Public membership projection. Never exposes persistence entities. */
public record IdentityMembership(
        UUID id,
        UUID userId,
        UUID clinicId,
        String role,
        String status,
        Set<String> permissions
) {
    public IdentityMembership {
        permissions = permissions == null ? Set.of() : Set.copyOf(permissions);
    }
}
