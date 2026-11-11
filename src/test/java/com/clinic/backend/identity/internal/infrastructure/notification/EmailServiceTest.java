package com.clinic.backend.identity.internal.infrastructure.notification;

import jakarta.mail.Session;
import jakarta.mail.internet.InternetAddress;
import jakarta.mail.internet.MimeMessage;
import org.junit.jupiter.api.Test;
import org.springframework.mail.javamail.JavaMailSender;

import java.util.Properties;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class EmailServiceTest {

    @Test
    void sendsInvitationWithAlignedSenderReplyToAndAntiLoopHeaders() throws Exception {
        JavaMailSender mailSender = mock(JavaMailSender.class);
        MimeMessage message = new MimeMessage(Session.getInstance(new Properties()));
        when(mailSender.createMimeMessage()).thenReturn(message);
        EmailService service = new EmailService(mailSender,
                "invites@clinic.example",
                "Clinic Management System",
                "support@clinic.example",
                "https://app.clinic.example/doctor/claim");

        service.sendDoctorInvitation("doctor@example.com", "Dr. Smith", "Smile Clinic", "token-123");

        verify(mailSender).send(message);
        InternetAddress from = (InternetAddress) message.getFrom()[0];
        assertThat(from.getAddress()).isEqualTo("invites@clinic.example");
        assertThat(from.getPersonal()).isEqualTo("Clinic Management System");
        assertThat(message.getReplyTo()[0].toString()).isEqualTo("support@clinic.example");
        assertThat(message.getSubject()).isEqualTo("Invitation to join Smile Clinic");
        assertThat(message.getHeader("Auto-Submitted", null)).isEqualTo("auto-generated");
        assertThat(message.getHeader("X-Auto-Response-Suppress", null)).isEqualTo("OOF, AutoReply");
    }
}
