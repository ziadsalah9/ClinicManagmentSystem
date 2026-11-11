package com.clinic.backend.identity.internal.infrastructure.persistence;

import com.clinic.backend.shared.AuditedJpaEntity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.Instant;

@Entity
@Table(name = "password_login_attempt", schema = "identity")
public class PasswordLoginAttemptJpaEntity extends AuditedJpaEntity {
    @Id
    @Column(name = "phone", nullable = false, length = 16)
    private String phone;

    @Column(name = "failed_attempts", nullable = false)
    private int failedAttempts;

    @Column(name = "window_started_at", nullable = false)
    private Instant windowStartedAt;

    @Column(name = "locked_until")
    private Instant lockedUntil;

    protected PasswordLoginAttemptJpaEntity() { }

    PasswordLoginAttemptJpaEntity(String phone, int failedAttempts, Instant windowStartedAt, Instant lockedUntil) {
        this.phone = phone;
        this.failedAttempts = failedAttempts;
        this.windowStartedAt = windowStartedAt;
        this.lockedUntil = lockedUntil;
    }

    void apply(com.clinic.backend.identity.internal.domain.PasswordLoginAttempt attempt) {
        this.failedAttempts = attempt.failedAttempts();
        this.windowStartedAt = attempt.windowStartedAt();
        this.lockedUntil = attempt.lockedUntil();
    }

    String getPhone() { return phone; }
    int getFailedAttempts() { return failedAttempts; }
    Instant getWindowStartedAt() { return windowStartedAt; }
    Instant getLockedUntil() { return lockedUntil; }
}
