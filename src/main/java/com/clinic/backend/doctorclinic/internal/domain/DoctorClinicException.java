package com.clinic.backend.doctorclinic.internal.domain;

public class DoctorClinicException extends RuntimeException {
    private final DoctorClinicError error;

    public DoctorClinicException(DoctorClinicError error, String message) {
        super(message);
        this.error = error;
    }

    public DoctorClinicError error() { return error; }
}
