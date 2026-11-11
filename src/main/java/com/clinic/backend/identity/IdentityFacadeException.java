package com.clinic.backend.identity;

/** Stable public exception contract for other business modules. */
public class IdentityFacadeException extends RuntimeException {
    private final String code;

    public IdentityFacadeException(String code, String message) {
        super(message);
        this.code = code;
    }

    public String code() {
        return code;
    }
}
