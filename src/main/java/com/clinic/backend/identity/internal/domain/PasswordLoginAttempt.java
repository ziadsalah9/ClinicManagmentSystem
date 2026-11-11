package com.clinic.backend.identity.internal.domain;

import java.time.Instant;

public record PasswordLoginAttempt(String phone, int failedAttempts, Instant windowStartedAt, Instant lockedUntil) {
    public boolean isLockedAt(Instant now) {
        return lockedUntil != null && lockedUntil.isAfter(now);
    }

    public PasswordLoginAttempt failed(Instant now, int maxAttempts, java.time.Duration window, java.time.Duration lockDuration) {
        boolean resetWindow = windowStartedAt == null || !windowStartedAt.plus(window).isAfter(now);
        int nextCount = resetWindow ? 1 : failedAttempts + 1;
        Instant nextWindowStart = resetWindow ? now : windowStartedAt;
        Instant nextLock = nextCount >= maxAttempts ? now.plus(lockDuration) : null;
        return new PasswordLoginAttempt(phone, nextCount, nextWindowStart, nextLock);
    }
}
