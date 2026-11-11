package com.clinic.backend.doctor.internal.infrastructure.persistence;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Collection;
import java.util.List;
import java.util.UUID;

public interface SpringDataSpecialtyRepository extends JpaRepository<SpecialtyJpaEntity, UUID> {
    List<SpecialtyJpaEntity> findAllByActiveTrueOrderByNameEnAsc();
    List<SpecialtyJpaEntity> findAllByIdInAndActiveTrue(Collection<UUID> ids);
}
