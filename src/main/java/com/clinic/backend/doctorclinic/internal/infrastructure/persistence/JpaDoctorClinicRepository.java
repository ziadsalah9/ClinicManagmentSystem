package com.clinic.backend.doctorclinic.internal.infrastructure.persistence;

import com.clinic.backend.doctorclinic.internal.application.DoctorClinicRepository;
import com.clinic.backend.doctorclinic.internal.domain.DoctorClinic;
import com.clinic.backend.doctorclinic.internal.domain.DoctorClinicStatus;
import jakarta.persistence.EntityManager;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
@Transactional(readOnly = true)
public class JpaDoctorClinicRepository implements DoctorClinicRepository {
    private final SpringDataDoctorClinicRepository associations;
    private final EntityManager entityManager;

    public JpaDoctorClinicRepository(SpringDataDoctorClinicRepository associations, EntityManager entityManager) {
        this.associations = associations;
        this.entityManager = entityManager;
    }

    @Override
    @Transactional(propagation = Propagation.MANDATORY)
    public void lockAssociation(UUID clinicId, UUID doctorId) {
        String lockKey = "doctor-clinic:" + clinicId + ":" + doctorId;
        entityManager.createNativeQuery("select pg_advisory_xact_lock(hashtextextended(:lockKey, 0))")
                .setParameter("lockKey", lockKey)
                .getResultList();
    }

    @Override
    public Optional<DoctorClinic> findById(UUID id) {
        return associations.findById(id).map(DoctorClinicJpaEntity::toDomain);
    }

    @Override
    public Optional<DoctorClinic> findByClinicAndDoctor(UUID clinicId, UUID doctorId) {
        return associations.findByClinicIdAndDoctorId(clinicId, doctorId).map(DoctorClinicJpaEntity::toDomain);
    }

    @Override
    public List<DoctorClinic> findByClinicId(UUID clinicId) {
        return associations.findAllByClinicIdAndStatusOrderByDoctorId(clinicId, DoctorClinicStatus.ACTIVE)
                .stream().map(DoctorClinicJpaEntity::toDomain).toList();
    }

    @Override
    public List<DoctorClinic> findByDoctorId(UUID doctorId) {
        return associations.findAllByDoctorIdAndStatusOrderByClinicId(doctorId, DoctorClinicStatus.ACTIVE)
                .stream().map(DoctorClinicJpaEntity::toDomain).toList();
    }

    @Override
    public List<DoctorClinic> findAllActive() {
        return associations.findAllByStatusOrderByClinicIdAscDoctorIdAsc(DoctorClinicStatus.ACTIVE)
                .stream().map(DoctorClinicJpaEntity::toDomain).toList();
    }

    @Override
    @Transactional
    public DoctorClinic save(DoctorClinic association) {
        DoctorClinicJpaEntity entity = associations.findByClinicIdAndDoctorId(
                        association.clinicId(), association.doctorId())
                .orElseGet(() -> new DoctorClinicJpaEntity(association));
        entity.apply(association);
        return associations.saveAndFlush(entity).toDomain();
    }
}
