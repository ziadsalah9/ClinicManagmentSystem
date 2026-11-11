package com.clinic.backend.identity.internal.infrastructure.notification;

import com.clinic.backend.identity.internal.application.port.InvitationDeliveryPort;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

import java.time.Instant;

@Component
@Profile("!local")
public class UnconfiguredInvitationDeliveryAdapter implements InvitationDeliveryPort {
    @Override
    public void send(String phone, String invitationToken, Instant expiresAt) {
        throw new IllegalStateException("No production invitation provider is configured. Implement InvitationDeliveryPort.");
    }
}
