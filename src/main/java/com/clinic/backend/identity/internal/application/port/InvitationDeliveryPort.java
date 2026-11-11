package com.clinic.backend.identity.internal.application.port;

import java.time.Instant;

public interface InvitationDeliveryPort {
    void send(String phone, String invitationToken, Instant expiresAt);
}
