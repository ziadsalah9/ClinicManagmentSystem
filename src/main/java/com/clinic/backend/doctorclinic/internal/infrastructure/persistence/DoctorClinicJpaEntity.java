package com.clinic.backend.doctorclinic.internal.infrastructure.persistence;

import com.clinic.backend.doctorclinic.internal.domain.DoctorClinic;
import com.clinic.backend.doctorclinic.internal.domain.DoctorClinicStatus;
import com.clinic.backend.shared.AuditedJpaEntity;
import jakarta.persistence.*;

import java.math.BigDecimal;
import java.util.UUID;

@Entity
@Table(name = "doctor_clinic", schema = "doctor_clinic",
        uniqueConstraints = @UniqueConstraint(name = "uk_doctor_clinic_pair", columnNames = {"clinic_id", "doctor_id"}))
public class DoctorClinicJpaEntity extends AuditedJpaEntity {
    @Id
    private UUID id;

    @Column(name = "clinic_id", nullable = false, updatable = false)
    private UUID clinicId;

    @Column(name = "doctor_id", nullable = false, updatable = false)
    private UUID doctorId;

    @Column(name = "consultation_fee", nullable = false, precision = 12, scale = 2)
    private BigDecimal consultationFee;

    @Column(name = "follow_up_fee", nullable = false, precision = 12, scale = 2)
    private BigDecimal followUpFee;

    @Column(name = "follow_up_validity_days", nullable = false)
    private int followUpValidityDays;

    @Column(name = "instapay_address", length = 150)
    private String instaPayAddress;

    @Column(name = "auto_confirm", nullable = false)
    private boolean autoConfirm;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private DoctorClinicStatus status;

    protected DoctorClinicJpaEntity() { }

    DoctorClinicJpaEntity(DoctorClinic association) {
        this.id = association.id();
        this.clinicId = association.clinicId();
        this.doctorId = association.doctorId();
        apply(association);
    }

    void apply(DoctorClinic association) {
        this.consultationFee = association.consultationFee();
        this.followUpFee = association.followUpFee();
        this.followUpValidityDays = association.followUpValidityDays();
        this.instaPayAddress = association.instaPayAddress();
        this.autoConfirm = association.autoConfirm();
        this.status = association.status();
    }

    DoctorClinic toDomain() {
        return new DoctorClinic(id, doctorId, clinicId, consultationFee, followUpFee,
                followUpValidityDays, instaPayAddress, autoConfirm, status, getVersion());
    }

    public UUID getId() { return id; }
}
