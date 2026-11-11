package com.clinic.backend.identity.internal.infrastructure.notification;

import com.clinic.backend.identity.internal.application.port.InvitationDeliveryPort;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

import java.time.Instant;

@Component
@Profile("local")
public class LocalInvitationConsoleAdapter implements InvitationDeliveryPort {
    private static final Logger log = LoggerFactory.getLogger(LocalInvitationConsoleAdapter.class);

    @Override
    public void send(String phone, String invitationToken, Instant expiresAt) {
        // Development-only. A production SMS/deep-link provider must replace this adapter.
        log.warn("LOCAL INVITATION for {} token={} expiresAt={}", phone, invitationToken, expiresAt);
    }
}
