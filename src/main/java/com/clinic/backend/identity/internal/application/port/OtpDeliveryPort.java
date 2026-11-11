package com.clinic.backend.identity.internal.application.port;

import java.util.UUID;

public interface OtpDeliveryPort {
    void send(UUID challengeId, String phone, String code);
}
