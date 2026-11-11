package com.clinic.backend.clinic.internal.domain;

public class ClinicException extends RuntimeException {
    private final ClinicError error;

    public ClinicException(ClinicError error, String message) {
        super(message);
        this.error = error;
    }

    public ClinicError error() { return error; }
}
