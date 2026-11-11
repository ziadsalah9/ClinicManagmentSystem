package com.clinic.backend.identity.internal.infrastructure.persistence;

import com.clinic.backend.shared.AuditedJpaEntity;

import com.clinic.backend.identity.internal.domain.MembershipStatus;
import com.clinic.backend.identity.internal.domain.StaffRole;
import jakarta.persistence.*;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

@Entity
@Table(name = "staff_membership", schema = "identity",
        uniqueConstraints = @UniqueConstraint(name = "uk_identity_membership_clinic_user", columnNames = {"clinic_id", "user_id"}))
public class StaffMembershipJpaEntity extends AuditedJpaEntity {
    @Id
    private UUID id;

    @Column(name = "user_id", nullable = false)
    private UUID userId;

    // Intentionally no FK to the Clinic module: cross-module table ownership is forbidden.
    @Column(name = "clinic_id", nullable = false)
    private UUID clinicId;

    @Enumerated(EnumType.STRING)
    @Column(name = "role", nullable = false, length = 32)
    private StaffRole role;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 32)
    private MembershipStatus status;

    @ElementCollection(fetch = FetchType.LAZY)
    @CollectionTable(name = "membership_permission", schema = "identity",
            joinColumns = @JoinColumn(name = "membership_id"))
    @Column(name = "permission", nullable = false, length = 64)
    private Set<String> permissions = new HashSet<>();

    protected StaffMembershipJpaEntity() { }

    public StaffMembershipJpaEntity(UUID id, UUID userId, UUID clinicId, StaffRole role,
                                    MembershipStatus status, Set<String> permissions) {
        this.id = id;
        this.userId = userId;
        this.clinicId = clinicId;
        this.role = role;
        this.status = status;
        this.permissions = permissions == null ? new HashSet<>() : new HashSet<>(permissions);
    }

    public UUID getId() { return id; }
    public UUID getUserId() { return userId; }
    public UUID getClinicId() { return clinicId; }
    public StaffRole getRole() { return role; }
    public MembershipStatus getStatus() { return status; }
    public Set<String> getPermissions() { return permissions; }
}
