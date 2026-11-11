package com.clinic.backend.identity.internal.infrastructure.notification;

import com.clinic.backend.identity.internal.application.port.OtpDeliveryPort;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

import java.util.UUID;

@Component
@Profile("local")
public class LocalOtpConsoleAdapter implements OtpDeliveryPort {
    private static final Logger log = LoggerFactory.getLogger(LocalOtpConsoleAdapter.class);
    private final LocalOtpStore store;

    public LocalOtpConsoleAdapter(LocalOtpStore store) {
        this.store = store;
    }

    @Override
    public void send(UUID challengeId, String phone, String code) {
        store.record(challengeId, phone, code);
        // Development-only. Never log OTPs in staging or production.
        log.warn("LOCAL OTP for {} is {} (challengeId={}). This adapter must never be enabled in production.", phone, code, challengeId);
    }
}
