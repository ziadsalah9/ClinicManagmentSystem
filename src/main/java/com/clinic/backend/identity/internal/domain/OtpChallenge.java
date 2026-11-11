package com.clinic.backend.identity.internal.domain;

import java.time.Instant;
import java.util.UUID;

public record OtpChallenge(
        UUID id,
        String phone,
        OtpPurpose purpose,
        String codeHash,
        int attemptCount,
        Instant expiresAt,
        String verifiedTokenHash,
        Instant verifiedTokenExpiresAt,
        Instant consumedAt
) {
    public static OtpChallenge create(String phone, OtpPurpose purpose,
                                      String codeHash, Instant expiresAt) {
        return new OtpChallenge(UUID.randomUUID(), phone, purpose, codeHash,
                0, expiresAt, null, null, null);
    }

    public boolean isUsableAt(Instant now, int maxAttempts) {
        return consumedAt == null && expiresAt.isAfter(now) && attemptCount < maxAttempts;
    }

    public OtpChallenge failedAttempt() {
        return new OtpChallenge(id, phone, purpose, codeHash, attemptCount + 1,
                expiresAt, verifiedTokenHash, verifiedTokenExpiresAt, consumedAt);
    }

    public OtpChallenge verified(String tokenHash, Instant tokenExpiry) {
        return new OtpChallenge(id, phone, purpose, codeHash, attemptCount,
                expiresAt, tokenHash, tokenExpiry, consumedAt);
    }

    public OtpChallenge consumed(Instant now) {
        return new OtpChallenge(id, phone, purpose, codeHash, attemptCount,
                expiresAt, verifiedTokenHash, verifiedTokenExpiresAt, now);
    }

    public boolean hasUsableVerificationTokenAt(Instant now) {
        return verifiedTokenHash != null && verifiedTokenExpiresAt != null
                && verifiedTokenExpiresAt.isAfter(now) && consumedAt == null;
    }
}
