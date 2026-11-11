package com.clinic.backend.identity.internal.infrastructure.persistence;

import com.clinic.backend.identity.internal.domain.DoctorOnboardingInvite;
import com.clinic.backend.identity.internal.domain.DoctorOnboardingStatus;
import com.clinic.backend.shared.AuditedJpaEntity;
import jakarta.persistence.*;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "doctor_onboarding_invite", schema = "identity")
public class DoctorOnboardingInviteJpaEntity extends AuditedJpaEntity {
    @Id
    private UUID id;
    @Column(name = "clinic_id", nullable = false)
    private UUID clinicId;
    @Column(name = "phone", nullable = false, length = 16)
    private String phone;
    @Column(name = "full_name", nullable = false, length = 150)
    private String fullName;
    @Column(name = "email", length = 254)
    private String email;
    @Column(name = "token_hash", nullable = false, unique = true, length = 64)
    private String tokenHash;
    @Column(name = "invited_by_user_id", nullable = false)
    private UUID invitedByUserId;
    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 32)
    private DoctorOnboardingStatus status;
    @Column(name = "attempt_count", nullable = false)
    private int attemptCount;
    @Column(name = "expires_at", nullable = false)
    private Instant expiresAt;
    @Column(name = "claimed_at")
    private Instant claimedAt;
    @Column(name = "claimed_user_id")
    private UUID claimedUserId;

    protected DoctorOnboardingInviteJpaEntity() {
    }

    public DoctorOnboardingInviteJpaEntity(DoctorOnboardingInvite d) {
        this.id = d.id();
        apply(d);
    }

    public void apply(DoctorOnboardingInvite d) {
        this.clinicId = d.clinicId();
        this.phone = d.phone();
        this.fullName = d.fullName();
        this.email = d.email();
        this.tokenHash = d.tokenHash();
        this.invitedByUserId = d.invitedByUserId();
        this.status = d.status();
        this.attemptCount = d.attemptCount();
        this.expiresAt = d.expiresAt();
        this.claimedAt = d.claimedAt();
        this.claimedUserId = d.claimedUserId();
    }

    public DoctorOnboardingInvite toDomain() {
        return new DoctorOnboardingInvite(id, clinicId, phone, fullName, email, tokenHash, invitedByUserId, status,
                attemptCount, expiresAt, claimedAt, claimedUserId);
    }

    public UUID getId() {
        return id;
    }
}
