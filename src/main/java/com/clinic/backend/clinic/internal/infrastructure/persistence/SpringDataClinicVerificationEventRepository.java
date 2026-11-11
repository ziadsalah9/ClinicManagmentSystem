package com.clinic.backend.clinic.internal.infrastructure.persistence;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface SpringDataClinicVerificationEventRepository
        extends JpaRepository<ClinicVerificationEventJpaEntity, UUID> {
    List<ClinicVerificationEventJpaEntity> findByClinicIdOrderByCreatedAtDesc(UUID clinicId);
}
