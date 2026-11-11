package com.clinic.backend.doctor.internal.infrastructure.persistence;

import com.clinic.backend.doctor.internal.domain.DoctorGender;
import com.clinic.backend.doctor.internal.domain.DoctorProfile;
import com.clinic.backend.doctor.internal.domain.DoctorVerificationStatus;
import com.clinic.backend.shared.AuditedJpaEntity;
import jakarta.persistence.*;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

@Entity
@Table(name = "doctor_profile", schema = "doctor")
public class DoctorProfileJpaEntity extends AuditedJpaEntity {
    @Id
    private UUID id;

    @Column(name = "user_id", nullable = false, updatable = false)
    private UUID userId;

    @Column(name = "professional_title", nullable = false, length = 120)
    private String professionalTitle;

    @Column(name = "bio", length = 2000)
    private String bio;

    @Column(name = "years_experience", nullable = false)
    private int yearsExperience;

    @Column(name = "syndicate_number", length = 50)
    private String syndicateNumber;

    @Enumerated(EnumType.STRING)
    @Column(name = "gender", nullable = false, length = 20)
    private DoctorGender gender;

    @Enumerated(EnumType.STRING)
    @Column(name = "verification_status", nullable = false, length = 32)
    private DoctorVerificationStatus verificationStatus;

    @ElementCollection(fetch = FetchType.LAZY)
    @CollectionTable(name = "doctor_specialty", schema = "doctor",
            joinColumns = @JoinColumn(name = "doctor_id"))
    @Column(name = "specialty_id", nullable = false)
    private Set<UUID> specialtyIds = new HashSet<>();

    protected DoctorProfileJpaEntity() { }

    DoctorProfileJpaEntity(DoctorProfile doctor) {
        this.id = doctor.id();
        this.userId = doctor.userId();
        apply(doctor);
    }

    void apply(DoctorProfile doctor) {
        this.professionalTitle = doctor.professionalTitle();
        this.bio = doctor.bio();
        this.yearsExperience = doctor.yearsExperience();
        this.syndicateNumber = doctor.syndicateNumber();
        this.gender = doctor.gender();
        this.verificationStatus = doctor.verificationStatus();
        this.specialtyIds.clear();
        this.specialtyIds.addAll(doctor.specialtyIds());
    }

    DoctorProfile toDomain() {
        return new DoctorProfile(id, userId, professionalTitle, bio, yearsExperience,
                syndicateNumber, gender, verificationStatus, specialtyIds, getVersion());
    }

    public UUID getId() { return id; }
    public UUID getUserId() { return userId; }
}
