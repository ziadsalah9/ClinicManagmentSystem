package com.clinic.backend.clinic.internal.infrastructure.persistence;

import com.clinic.backend.clinic.internal.domain.Area;
import com.clinic.backend.shared.AuditedJpaEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.util.UUID;

@Entity
@Table(name = "area", schema = "clinic")
public class AreaJpaEntity extends AuditedJpaEntity {
    @Id
    private UUID id;

    @Column(name = "code", nullable = false, length = 60, updatable = false)
    private String code;

    @Column(name = "name_en", nullable = false, length = 120)
    private String nameEn;

    @Column(name = "name_ar", nullable = false, length = 120)
    private String nameAr;

    @Column(name = "city", nullable = false, length = 100)
    private String city;

    @Column(name = "active", nullable = false)
    private boolean active;

    protected AreaJpaEntity() { }

    Area toDomain() {
        return new Area(id, code, nameEn, nameAr, city, active);
    }

    public UUID getId() { return id; }
}
