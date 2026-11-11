package com.clinic.backend.scheduling.infrastructure.persistence;

import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

public interface SpringDataScheduleExceptionRepository extends JpaRepository<ScheduleExceptionJpaEntity, UUID> {
    List<ScheduleExceptionJpaEntity> findAllByDoctorClinicIdOrderByDateFromAscDateToAsc(UUID doctorClinicId);

    List<ScheduleExceptionJpaEntity> findAllByDoctorClinicIdAndCancelledAtIsNullAndDateFromLessThanEqualAndDateToGreaterThanEqualOrderByDateFromAsc(
            UUID doctorClinicId, LocalDate dateTo, LocalDate dateFrom);
}
