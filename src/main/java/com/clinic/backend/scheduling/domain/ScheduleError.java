package com.clinic.backend.scheduling.domain;

public enum ScheduleError {
    INVALID_INPUT,
    ACCESS_DENIED,
    DOCTOR_CLINIC_NOT_FOUND,
    SHIFT_NOT_FOUND,
    EXCEPTION_NOT_FOUND,
    STALE_VERSION,
    SHIFT_NAME_CONFLICT,
    SHIFT_CONFLICT,
    BOOKING_CONFLICT,
    INVALID_DATE_RANGE
}
