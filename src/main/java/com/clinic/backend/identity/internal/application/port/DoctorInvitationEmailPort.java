package com.clinic.backend.identity.internal.application.port;

/**
 * Delivers the lightweight doctor onboarding claim token to the invited doctor's email.
 * Implementations must never log or otherwise expose the raw claim token.
 */
public interface DoctorInvitationEmailPort {
    void sendDoctorInvitation(String toEmail, String doctorName, String clinicName, String claimToken);
}
