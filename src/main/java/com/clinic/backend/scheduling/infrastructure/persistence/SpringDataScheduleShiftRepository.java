package com.clinic.backend.scheduling.infrastructure.persistence;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface SpringDataScheduleShiftRepository extends JpaRepository<ScheduleShiftJpaEntity, UUID> {
    List<ScheduleShiftJpaEntity> findAllByDoctorClinicIdOrderByNameAsc(UUID doctorClinicId);

    List<ScheduleShiftJpaEntity> findAllByDoctorClinicIdAndActiveOrderByNameAsc(UUID doctorClinicId, boolean active);
}
