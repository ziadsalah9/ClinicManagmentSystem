package com.clinic.backend.identity.internal.infrastructure.persistence;

import com.clinic.backend.shared.AuditedJpaEntity;

import jakarta.persistence.*;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "refresh_session", schema = "identity")
public class RefreshSessionJpaEntity extends AuditedJpaEntity {
    @Id
    private UUID id;

    @Column(name = "user_id", nullable = false)
    private UUID userId;

    @Column(name = "token_hash", nullable = false, unique = true, length = 64)
    private String tokenHash;

    @Column(name = "expires_at", nullable = false)
    private Instant expiresAt;

    @Column(name = "revoked_at")
    private Instant revokedAt;

    @Column(name = "replaced_by_session_id")
    private UUID replacedBySessionId;

    @Column(name = "device_id", length = 128)
    private String deviceId;

    protected RefreshSessionJpaEntity() { }

    public RefreshSessionJpaEntity(UUID id, UUID userId, String tokenHash, Instant expiresAt,
                                   Instant revokedAt, UUID replacedBySessionId, String deviceId) {
        this.id = id;
        this.userId = userId;
        this.tokenHash = tokenHash;
        this.expiresAt = expiresAt;
        this.revokedAt = revokedAt;
        this.replacedBySessionId = replacedBySessionId;
        this.deviceId = deviceId;
    }

    public void apply(com.clinic.backend.identity.internal.domain.RefreshSession session) {
        this.revokedAt = session.revokedAt();
        this.replacedBySessionId = session.replacedBySessionId();
    }

    public UUID getId() { return id; }
    public UUID getUserId() { return userId; }
    public String getTokenHash() { return tokenHash; }
    public Instant getExpiresAt() { return expiresAt; }
    public Instant getRevokedAt() { return revokedAt; }
    public UUID getReplacedBySessionId() { return replacedBySessionId; }
    public String getDeviceId() { return deviceId; }
}
