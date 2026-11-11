package com.clinic.backend.doctor.internal.infrastructure.persistence;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface SpringDataDoctorProfileRepository extends JpaRepository<DoctorProfileJpaEntity, UUID> {
    @EntityGraph(attributePaths = "specialtyIds")
    Optional<DoctorProfileJpaEntity> findWithSpecialtiesById(UUID id);

    @EntityGraph(attributePaths = "specialtyIds")
    Optional<DoctorProfileJpaEntity> findWithSpecialtiesByUserId(UUID userId);

    @EntityGraph(attributePaths = "specialtyIds")
    List<DoctorProfileJpaEntity> findWithSpecialtiesByIdIn(Collection<UUID> ids);
}
