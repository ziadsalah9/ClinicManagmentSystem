package com.clinic.backend.identity.internal.infrastructure.persistence;

import com.clinic.backend.shared.AuditedJpaEntity;

import com.clinic.backend.identity.internal.domain.OtpPurpose;
import jakarta.persistence.*;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "otp_challenge", schema = "identity")
public class OtpChallengeJpaEntity extends AuditedJpaEntity {
    @Id
    private UUID id;

    @Column(name = "phone", nullable = false, length = 16)
    private String phone;

    @Enumerated(EnumType.STRING)
    @Column(name = "purpose", nullable = false, length = 32)
    private OtpPurpose purpose;

    @Column(name = "code_hash", nullable = false, length = 100)
    private String codeHash;

    @Column(name = "attempt_count", nullable = false)
    private int attemptCount;

    @Column(name = "expires_at", nullable = false)
    private Instant expiresAt;

    @Column(name = "verified_token_hash", length = 64, unique = true)
    private String verifiedTokenHash;

    @Column(name = "verified_token_expires_at")
    private Instant verifiedTokenExpiresAt;

    @Column(name = "consumed_at")
    private Instant consumedAt;

    protected OtpChallengeJpaEntity() { }

    public OtpChallengeJpaEntity(UUID id, String phone, OtpPurpose purpose, String codeHash,
                                 int attemptCount, Instant expiresAt, String verifiedTokenHash,
                                 Instant verifiedTokenExpiresAt, Instant consumedAt) {
        this.id = id;
        this.phone = phone;
        this.purpose = purpose;
        this.codeHash = codeHash;
        this.attemptCount = attemptCount;
        this.expiresAt = expiresAt;
        this.verifiedTokenHash = verifiedTokenHash;
        this.verifiedTokenExpiresAt = verifiedTokenExpiresAt;
        this.consumedAt = consumedAt;
    }

    public void apply(com.clinic.backend.identity.internal.domain.OtpChallenge challenge) {
        this.attemptCount = challenge.attemptCount();
        this.verifiedTokenHash = challenge.verifiedTokenHash();
        this.verifiedTokenExpiresAt = challenge.verifiedTokenExpiresAt();
        this.consumedAt = challenge.consumedAt();
    }

    public UUID getId() { return id; }
    public String getPhone() { return phone; }
    public OtpPurpose getPurpose() { return purpose; }
    public String getCodeHash() { return codeHash; }
    public int getAttemptCount() { return attemptCount; }
    public Instant getExpiresAt() { return expiresAt; }
    public String getVerifiedTokenHash() { return verifiedTokenHash; }
    public Instant getVerifiedTokenExpiresAt() { return verifiedTokenExpiresAt; }
    public Instant getConsumedAt() { return consumedAt; }
}
