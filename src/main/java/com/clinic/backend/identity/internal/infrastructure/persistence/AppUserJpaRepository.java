package com.clinic.backend.identity.internal.infrastructure.persistence;

import com.clinic.backend.identity.internal.domain.SystemRole;
import com.clinic.backend.identity.internal.domain.UserStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface AppUserJpaRepository extends JpaRepository<AppUserJpaEntity, UUID> {
    Optional<AppUserJpaEntity> findByPhone(String phone);

    @Query("select u.id from AppUserJpaEntity u where u.systemRole = :role and u.status = :status")
    List<UUID> findIdsBySystemRoleAndStatus(SystemRole role, UserStatus status);
}
