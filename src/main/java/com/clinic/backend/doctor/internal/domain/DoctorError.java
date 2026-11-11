package com.clinic.backend.doctor.internal.domain;

public enum DoctorError {
    INVALID_INPUT,
    DOCTOR_PROFILE_NOT_FOUND,
    DOCTOR_PROFILE_ALREADY_EXISTS,
    STALE_VERSION,
    NOT_A_DOCTOR,
    SPECIALTY_NOT_FOUND,
    ACCESS_DENIED
}
