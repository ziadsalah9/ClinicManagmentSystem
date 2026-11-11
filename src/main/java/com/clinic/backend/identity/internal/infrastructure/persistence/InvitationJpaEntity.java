package com.clinic.backend.identity.internal.infrastructure.persistence;

import com.clinic.backend.shared.AuditedJpaEntity;

import com.clinic.backend.identity.internal.domain.InvitationStatus;
import com.clinic.backend.identity.internal.domain.StaffRole;
import jakarta.persistence.*;

import java.time.Instant;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

@Entity
@Table(name = "staff_invitation", schema = "identity")
public class InvitationJpaEntity extends AuditedJpaEntity {
    @Id
    private UUID id;

    @Column(name = "clinic_id", nullable = false)
    private UUID clinicId;

    @Column(name = "phone", nullable = false, length = 16)
    private String phone;

    @Enumerated(EnumType.STRING)
    @Column(name = "role", nullable = false, length = 32)
    private StaffRole role;

    @Column(name = "token_hash", nullable = false, unique = true, length = 64)
    private String tokenHash;

    @Column(name = "invited_by_user_id", nullable = false)
    private UUID invitedByUserId;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 32)
    private InvitationStatus status;

    @Column(name = "expires_at", nullable = false)
    private Instant expiresAt;

    @Column(name = "accepted_at")
    private Instant acceptedAt;

    @ElementCollection(fetch = FetchType.LAZY)
    @CollectionTable(name = "invitation_permission", schema = "identity",
            joinColumns = @JoinColumn(name = "invitation_id"))
    @Column(name = "permission", nullable = false, length = 64)
    private Set<String> permissions = new HashSet<>();

    protected InvitationJpaEntity() { }

    public InvitationJpaEntity(UUID id, UUID clinicId, String phone, StaffRole role,
                               Set<String> permissions, String tokenHash, UUID invitedByUserId,
                               InvitationStatus status, Instant expiresAt, Instant acceptedAt) {
        this.id = id;
        this.clinicId = clinicId;
        this.phone = phone;
        this.role = role;
        this.permissions = permissions == null ? new HashSet<>() : new HashSet<>(permissions);
        this.tokenHash = tokenHash;
        this.invitedByUserId = invitedByUserId;
        this.status = status;
        this.expiresAt = expiresAt;
        this.acceptedAt = acceptedAt;
    }

    public void apply(com.clinic.backend.identity.internal.domain.Invitation invitation) {
        this.status = invitation.status();
        this.acceptedAt = invitation.acceptedAt();
    }

    public UUID getId() { return id; }
    public UUID getClinicId() { return clinicId; }
    public String getPhone() { return phone; }
    public StaffRole getRole() { return role; }
    public Set<String> getPermissions() { return permissions; }
    public String getTokenHash() { return tokenHash; }
    public UUID getInvitedByUserId() { return invitedByUserId; }
    public InvitationStatus getStatus() { return status; }
    public Instant getExpiresAt() { return expiresAt; }
    public Instant getAcceptedAt() { return acceptedAt; }
}
