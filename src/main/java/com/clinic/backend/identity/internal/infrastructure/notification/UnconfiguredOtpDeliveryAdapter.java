package com.clinic.backend.identity.internal.infrastructure.notification;

import com.clinic.backend.identity.internal.application.port.OtpDeliveryPort;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

import java.util.UUID;

@Component
@Profile("!local")
public class UnconfiguredOtpDeliveryAdapter implements OtpDeliveryPort {
    @Override
    public void send(UUID challengeId, String phone, String code) {
        throw new IllegalStateException("No production OTP provider is configured. Implement OtpDeliveryPort.");
    }
}
