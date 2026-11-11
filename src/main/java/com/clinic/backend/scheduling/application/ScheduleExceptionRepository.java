package com.clinic.backend.scheduling.application;

import com.clinic.backend.scheduling.domain.model.ScheduleException;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ScheduleExceptionRepository {
    Optional<StoredException> findById(UUID exceptionId);

    List<StoredException> findByDoctorClinicId(UUID doctorClinicId);

    List<StoredException> findActiveOverlapping(UUID doctorClinicId, LocalDate from, LocalDate to);

    StoredException save(ScheduleException exception);

    record StoredException(ScheduleException exception, long version, Instant createdAt, Instant updatedAt) { }
}
