package com.clinic.backend.doctorclinic.internal.domain;

public enum DoctorClinicError {
    INVALID_INPUT,
    CLINIC_NOT_FOUND,
    DOCTOR_NOT_FOUND,
    DOCTOR_NOT_MEMBER_OF_CLINIC,
    ACCESS_DENIED,
    STALE_VERSION,
    RELATIONSHIP_CONFLICT,
    ASSOCIATION_NOT_FOUND,
    CLINIC_NOT_AVAILABLE
}
