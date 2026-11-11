package com.clinic.backend.identity.internal.infrastructure.persistence;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;
public interface DoctorOnboardingInviteJpaRepository extends JpaRepository<DoctorOnboardingInviteJpaEntity, UUID> {
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select i from DoctorOnboardingInviteJpaEntity i where i.tokenHash = :tokenHash")
    Optional<DoctorOnboardingInviteJpaEntity> findByTokenHashForUpdate(@Param("tokenHash") String tokenHash);
    long countByPhoneAndCreatedAtAfter(String phone, Instant since);
}
