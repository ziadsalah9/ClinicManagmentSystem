package com.clinic.backend.scheduling.application;

import com.clinic.backend.scheduling.domain.model.ScheduleShift;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ScheduleShiftRepository {
    Optional<StoredShift> findById(UUID shiftId);

    List<StoredShift> findByDoctorClinicId(UUID doctorClinicId);

    List<StoredShift> findActiveByDoctorClinicId(UUID doctorClinicId);

    StoredShift save(ScheduleShift shift);

    record StoredShift(ScheduleShift shift, long version, Instant createdAt, Instant updatedAt) { }
}
