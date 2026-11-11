package com.clinic.backend.identity.internal.infrastructure.persistence;

import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

public interface InvitationJpaRepository extends JpaRepository<InvitationJpaEntity, UUID> {
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select i from InvitationJpaEntity i where i.tokenHash = :tokenHash")
    Optional<InvitationJpaEntity> findByTokenHashForUpdate(@Param("tokenHash") String tokenHash);

    @Modifying
    @Query("update InvitationJpaEntity i set i.status = com.clinic.backend.identity.internal.domain.InvitationStatus.EXPIRED " +
            "where i.clinicId = :clinicId and i.phone = :phone and i.status = com.clinic.backend.identity.internal.domain.InvitationStatus.PENDING and i.expiresAt <= :now")
    int expirePending(@Param("clinicId") UUID clinicId, @Param("phone") String phone, @Param("now") Instant now);

    boolean existsByClinicIdAndPhoneAndStatusAndExpiresAtAfter(
            UUID clinicId, String phone,
            com.clinic.backend.identity.internal.domain.InvitationStatus status,
            Instant now);
}
