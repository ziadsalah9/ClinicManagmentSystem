package com.clinic.backend.identity.internal.infrastructure.persistence;

import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

public interface OtpChallengeJpaRepository extends JpaRepository<OtpChallengeJpaEntity, UUID> {
    long countByPhoneAndCreatedAtAfter(String phone, Instant since);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select o from OtpChallengeJpaEntity o where o.id = :id")
    Optional<OtpChallengeJpaEntity> findByIdForUpdate(@Param("id") UUID id);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select o from OtpChallengeJpaEntity o where o.verifiedTokenHash = :hash and o.consumedAt is null")
    Optional<OtpChallengeJpaEntity> findByVerifiedTokenHashForUpdate(@Param("hash") String hash);
}
