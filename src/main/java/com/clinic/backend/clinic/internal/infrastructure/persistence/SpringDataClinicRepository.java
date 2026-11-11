package com.clinic.backend.clinic.internal.infrastructure.persistence;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface SpringDataClinicRepository extends JpaRepository<ClinicJpaEntity, UUID> {
    @EntityGraph(attributePaths = "phones")
    @Query("select distinct c from ClinicJpaEntity c where c.id = :id")
    Optional<ClinicJpaEntity> findWithPhonesById(@Param("id") UUID id);

    @EntityGraph(attributePaths = "phones")
    @Query("select distinct c from ClinicJpaEntity c where c.id in :ids")
    List<ClinicJpaEntity> findAllWithPhonesByIdIn(@Param("ids") Collection<UUID> ids);

    @EntityGraph(attributePaths = "phones")
    @Query("select distinct c from ClinicJpaEntity c order by c.createdAt desc")
    List<ClinicJpaEntity> findAllWithPhones();

    boolean existsByOwnerUserIdAndId(UUID ownerUserId, UUID id);
}
