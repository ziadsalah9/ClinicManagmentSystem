package com.clinic.backend.identity.internal.domain;

public class IdentityException extends RuntimeException {
    private final IdentityError error;

    public IdentityException(IdentityError error, String message) {
        super(message);
        this.error = error;
    }

    public IdentityError error() {
        return error;
    }
}
