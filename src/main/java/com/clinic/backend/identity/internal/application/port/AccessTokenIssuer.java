package com.clinic.backend.identity.internal.application.port;

import java.time.Instant;
import java.util.Set;
import java.util.UUID;

public interface AccessTokenIssuer {
    IssuedAccessToken issue(UUID userId, Set<String> roles);

    IssuedAccessToken issue(UUID userId, Set<String> roles, boolean mustChangePassword);

    record IssuedAccessToken(String value, Instant expiresAt) {
    }
}
