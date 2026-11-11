package com.clinic.backend.clinic.internal.infrastructure.persistence;

import com.clinic.backend.clinic.internal.domain.Clinic;
import com.clinic.backend.clinic.internal.domain.ClinicStatus;
import com.clinic.backend.shared.AuditedJpaEntity;
import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.Table;
import jakarta.persistence.FetchType;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

@Entity
@Table(name = "clinic", schema = "clinic")
public class ClinicJpaEntity extends AuditedJpaEntity {
    @Id
    private UUID id;

    @Column(name = "owner_user_id", nullable = false, updatable = false)
    private UUID ownerUserId;

    @Column(name = "area_id", nullable = false)
    private UUID areaId;

    @Column(name = "name", nullable = false, length = 150)
    private String name;

    @Column(name = "address", nullable = false, length = 300)
    private String address;

    @Column(name = "time_zone", nullable = false, length = 64)
    private String timeZone;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 32)
    private ClinicStatus status;

    @ElementCollection(fetch = FetchType.LAZY)
    @CollectionTable(name = "clinic_phone", schema = "clinic",
            joinColumns = @JoinColumn(name = "clinic_id"))
    @Column(name = "phone", nullable = false, length = 16)
    private Set<String> phones = new HashSet<>();

    protected ClinicJpaEntity() { }

    ClinicJpaEntity(Clinic clinic) {
        this.id = clinic.id();
        this.phones = new HashSet<>();
        apply(clinic);
    }

    void apply(Clinic clinic) {
        this.ownerUserId = clinic.ownerUserId();
        this.areaId = clinic.areaId();
        this.name = clinic.name();
        this.address = clinic.address();
        this.timeZone = clinic.timeZone();
        this.status = clinic.status();
        this.phones.clear();
        this.phones.addAll(clinic.phones());
    }

    Clinic toDomain() {
        return new Clinic(id, ownerUserId, areaId, name, address, timeZone, status,
                phones.stream().sorted().toList(), getVersion(), getCreatedAt());
    }
}
