package com.clinic.backend.doctorclinic;

import java.util.UUID;

/** Minimal doctor-clinic data needed by Scheduling without exposing other module internals. */
public record DoctorClinicScheduleInfo(
        UUID id,
        UUID clinicId,
        UUID doctorId,
        UUID doctorUserId,
        String clinicTimeZone,
        String status,
        long version
) { }
