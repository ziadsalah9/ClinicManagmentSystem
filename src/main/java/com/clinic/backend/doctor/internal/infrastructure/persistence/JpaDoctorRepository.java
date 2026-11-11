package com.clinic.backend.doctor.internal.infrastructure.persistence;

import com.clinic.backend.doctor.internal.application.DoctorRepository;
import com.clinic.backend.doctor.internal.domain.DoctorProfile;
import com.clinic.backend.doctor.internal.domain.Specialty;
import jakarta.persistence.EntityManager;
import jakarta.persistence.LockModeType;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

@Repository
@Transactional(readOnly = true)
public class JpaDoctorRepository implements DoctorRepository {
    private final SpringDataDoctorProfileRepository doctors;
    private final SpringDataSpecialtyRepository specialties;
    private final EntityManager entityManager;

    public JpaDoctorRepository(SpringDataDoctorProfileRepository doctors, SpringDataSpecialtyRepository specialties, EntityManager entityManager) {
        this.doctors = doctors;
        this.specialties = specialties;
        this.entityManager = entityManager;
    }

    @Override
    @Transactional(propagation = Propagation.MANDATORY)
    public void lockProfile(UUID userId) {
        String lockKey = "doctor-profile:" + userId;
        entityManager.createNativeQuery("select pg_advisory_xact_lock(hashtextextended(:lockKey, 0))")
                .setParameter("lockKey", lockKey)
                .getResultList();
    }

    @Override
    public Optional<DoctorProfile> findById(UUID doctorId) {
        return doctors.findWithSpecialtiesById(doctorId).map(DoctorProfileJpaEntity::toDomain);
    }

    @Override
    public Optional<DoctorProfile> findByUserId(UUID userId) {
        return doctors.findWithSpecialtiesByUserId(userId).map(DoctorProfileJpaEntity::toDomain);
    }

    @Override
    public List<DoctorProfile> findByIds(Collection<UUID> doctorIds) {
        if (doctorIds == null || doctorIds.isEmpty()) return List.of();
        return doctors.findWithSpecialtiesByIdIn(doctorIds).stream().map(DoctorProfileJpaEntity::toDomain).toList();
    }

    @Override
    @Transactional
    public DoctorProfile save(DoctorProfile doctor) {
        Optional<DoctorProfileJpaEntity> existing = doctors.findWithSpecialtiesById(doctor.id());
        DoctorProfileJpaEntity entity = existing.orElseGet(() -> new DoctorProfileJpaEntity(doctor));
        if (existing.isPresent()) {
            // A specialty-only update changes the collection table; force the profile version too.
            entityManager.lock(entity, LockModeType.PESSIMISTIC_FORCE_INCREMENT);
        }
        entity.apply(doctor);
        return doctors.saveAndFlush(entity).toDomain();
    }

    @Override
    public List<Specialty> findActiveSpecialties() {
        return specialties.findAllByActiveTrueOrderByNameEnAsc().stream().map(JpaDoctorRepository::toDomain).toList();
    }

    @Override
    public Set<UUID> findActiveSpecialtyIds(Set<UUID> ids) {
        if (ids == null || ids.isEmpty()) return Set.of();
        return specialties.findAllByIdInAndActiveTrue(ids).stream()
                .map(SpecialtyJpaEntity::getId).collect(Collectors.toUnmodifiableSet());
    }

    private static Specialty toDomain(SpecialtyJpaEntity entity) {
        return new Specialty(entity.getId(), entity.getCode(), entity.getNameEn(), entity.getNameAr(), entity.isActive());
    }
}
