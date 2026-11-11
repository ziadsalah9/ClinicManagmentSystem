package com.clinic.backend.identity.internal.infrastructure.persistence;

import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface PasswordLoginAttemptJpaRepository extends JpaRepository<PasswordLoginAttemptJpaEntity, String> {
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select a from PasswordLoginAttemptJpaEntity a where a.phone = :phone")
    Optional<PasswordLoginAttemptJpaEntity> findByPhoneForUpdate(@Param("phone") String phone);
}
