package com.clinic.backend.identity.internal.infrastructure.persistence;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import com.clinic.backend.identity.internal.domain.MembershipStatus;
import com.clinic.backend.identity.internal.domain.StaffRole;

import java.util.Optional;
import java.util.UUID;
import java.util.List;

public interface StaffMembershipJpaRepository extends JpaRepository<StaffMembershipJpaEntity, UUID> {
    @EntityGraph(attributePaths = "permissions")
    Optional<StaffMembershipJpaEntity> findByUserIdAndClinicId(UUID userId, UUID clinicId);

    boolean existsByUserIdAndClinicId(UUID userId, UUID clinicId);

    boolean existsByUserIdAndClinicIdAndRoleAndStatus(UUID userId, UUID clinicId, StaffRole role, MembershipStatus status);

    boolean existsByUserIdAndRoleAndStatus(UUID userId, StaffRole role, MembershipStatus status);

    List<StaffMembershipJpaEntity> findAllByUserIdAndStatus(UUID userId, MembershipStatus status);

    List<StaffMembershipJpaEntity> findAllByUserIdAndRoleAndStatus(UUID userId, StaffRole role, MembershipStatus status);
}
