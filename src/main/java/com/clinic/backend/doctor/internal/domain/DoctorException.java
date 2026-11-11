package com.clinic.backend.doctor.internal.domain;

public class DoctorException extends RuntimeException {
    private final DoctorError error;

    public DoctorException(DoctorError error, String message) {
        super(message);
        this.error = error;
    }

    public DoctorError error() { return error; }
}
