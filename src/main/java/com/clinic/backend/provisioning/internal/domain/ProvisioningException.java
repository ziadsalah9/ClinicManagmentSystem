package com.clinic.backend.provisioning.internal.domain;

public class ProvisioningException extends RuntimeException {
    private final ProvisioningError error;

    public ProvisioningException(ProvisioningError error, String message) {
        super(message);
        this.error = error;
    }

    public ProvisioningError error() {
        return error;
    }
}
