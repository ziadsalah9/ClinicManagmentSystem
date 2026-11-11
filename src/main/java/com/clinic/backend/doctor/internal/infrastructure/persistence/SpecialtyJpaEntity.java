package com.clinic.backend.doctor.internal.infrastructure.persistence;

import com.clinic.backend.shared.AuditedJpaEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.util.UUID;

@Entity
@Table(name = "specialty", schema = "doctor")
public class SpecialtyJpaEntity extends AuditedJpaEntity {
    @Id
    private UUID id;
    @Column(name = "code", nullable = false, unique = true, length = 50)
    private String code;
    @Column(name = "name_en", nullable = false, length = 120)
    private String nameEn;
    @Column(name = "name_ar", nullable = false, length = 120)
    private String nameAr;
    @Column(name = "active", nullable = false)
    private boolean active;

    protected SpecialtyJpaEntity() { }

    public UUID getId() { return id; }
    public String getCode() { return code; }
    public String getNameEn() { return nameEn; }
    public String getNameAr() { return nameAr; }
    public boolean isActive() { return active; }
}
