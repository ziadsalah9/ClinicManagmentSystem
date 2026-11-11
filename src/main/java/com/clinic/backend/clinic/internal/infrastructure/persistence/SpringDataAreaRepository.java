package com.clinic.backend.clinic.internal.infrastructure.persistence;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface SpringDataAreaRepository extends JpaRepository<AreaJpaEntity, UUID> {
    Optional<AreaJpaEntity> findByIdAndActiveTrue(UUID id);
    List<AreaJpaEntity> findAllByActiveTrueOrderByCityAscNameEnAsc();
    List<AreaJpaEntity> findAllByActiveTrueAndCityIgnoreCaseOrderByNameEnAsc(String city);
    List<AreaJpaEntity> findAllByIdIn(Collection<UUID> ids);
}
