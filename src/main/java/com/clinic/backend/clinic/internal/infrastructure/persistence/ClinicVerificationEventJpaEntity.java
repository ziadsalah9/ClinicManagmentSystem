package com.clinic.backend.clinic.internal.infrastructure.persistence;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "verification_event", schema = "clinic")
public class ClinicVerificationEventJpaEntity {
    @Id
    private UUID id;

    @Column(name = "clinic_id", nullable = false, updatable = false)
    private UUID clinicId;

    @Column(name = "actor_user_id", nullable = false, updatable = false)
    private UUID actorUserId;

    @Column(name = "action", nullable = false, length = 40, updatable = false)
    private String action;

    @Column(name = "previous_status", nullable = false, length = 32, updatable = false)
    private String previousStatus;

    @Column(name = "new_status", nullable = false, length = 32, updatable = false)
    private String newStatus;

    @Column(name = "reason", length = 1000, updatable = false)
    private String reason;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    protected ClinicVerificationEventJpaEntity() { }

    ClinicVerificationEventJpaEntity(com.clinic.backend.clinic.internal.domain.ClinicVerificationEvent event) {
        this.id = event.id();
        this.clinicId = event.clinicId();
        this.actorUserId = event.actorUserId();
        this.action = event.action();
        this.previousStatus = event.previousStatus();
        this.newStatus = event.newStatus();
        this.reason = event.reason();
        this.createdAt = event.createdAt();
    }

    com.clinic.backend.clinic.internal.domain.ClinicVerificationEvent toDomain() {
        return new com.clinic.backend.clinic.internal.domain.ClinicVerificationEvent(
                id, clinicId, actorUserId, action, previousStatus, newStatus, reason, createdAt);
    }
}
