package com.clinic.backend.identity.internal.infrastructure.persistence;

import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;
import java.util.UUID;

public interface RefreshSessionJpaRepository extends JpaRepository<RefreshSessionJpaEntity, UUID> {
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select r from RefreshSessionJpaEntity r where r.tokenHash = :hash and r.revokedAt is null")
    Optional<RefreshSessionJpaEntity> findActiveByTokenHashForUpdate(@Param("hash") String hash);
}
