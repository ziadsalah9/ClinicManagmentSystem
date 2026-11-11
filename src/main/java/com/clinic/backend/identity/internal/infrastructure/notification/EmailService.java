package com.clinic.backend.identity.internal.infrastructure.notification;

import com.clinic.backend.identity.internal.application.port.DoctorInvitationEmailPort;
import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.web.util.HtmlUtils;
import org.springframework.web.util.UriComponentsBuilder;

import java.io.UnsupportedEncodingException;
import java.nio.charset.StandardCharsets;
import java.util.Locale;

/**
 * Sends doctor onboarding invitation emails through the configured SMTP server.
 * The raw claim token is only written into the email body and is never logged.
 */
@Component
public class EmailService implements DoctorInvitationEmailPort {
    private static final Logger log = LoggerFactory.getLogger(EmailService.class);

    private final JavaMailSender mailSender;
    private final String fromAddress;
    private final String fromName;
    private final String replyToAddress;
    private final String claimPageUrl;

    public EmailService(JavaMailSender mailSender,
                        @Value("${app.doctor-invitation.from:${spring.mail.username:}}") String fromAddress,
                        @Value("${app.doctor-invitation.from-name:Clinic Management System}") String fromName,
                        @Value("${app.doctor-invitation.reply-to:}") String replyToAddress,
                        @Value("${app.doctor-invitation.claim-page-url:http://localhost:5173/doctor/claim}") String claimPageUrl) {
        this.mailSender = mailSender;
        this.fromAddress = fromAddress == null ? "" : fromAddress.trim();
        this.fromName = fromName == null ? "" : fromName.trim();
        this.replyToAddress = replyToAddress == null ? "" : replyToAddress.trim();
        this.claimPageUrl = claimPageUrl == null ? "" : claimPageUrl.trim();
    }

    @Override
    @Async("emailTaskExecutor")
    public void sendDoctorInvitation(String toEmail, String doctorName, String clinicName, String claimToken) {
        try {
            String claimUrl = UriComponentsBuilder.fromUriString(claimPageUrl)
                    .queryParam("token", claimToken)
                    .build()
                    .encode()
                    .toUriString();
            warnIfClaimUrlIsNotPublic(claimUrl);
            String safeDoctorName = HtmlUtils.htmlEscape(safe(doctorName, "Doctor"));
            String safeClinicName = HtmlUtils.htmlEscape(safe(clinicName, "the clinic"));

            String plainText = "Hello " + safe(doctorName, "Doctor") + ",\n\n"
                    + "You have been invited to join " + safe(clinicName, "the clinic") + " as a doctor.\n\n"
                    + "Complete your onboarding using this link:\n" + claimUrl + "\n\n"
                    + "If the link does not open, use this claim token:\n" + claimToken + "\n\n"
                    + "This email was sent because this address was added to a doctor invitation for "
                    + safe(clinicName, "the clinic") + ". If you were not expecting it, ignore this email.\n";

            String html = """
                    <!doctype html>
                    <html lang="en">
                    <body style="margin:0;padding:0;background:#f4f6f8;font-family:Arial,Helvetica,sans-serif;color:#1f2937;">
                      <table role="presentation" width="100%%" cellspacing="0" cellpadding="0" style="background:#f4f6f8;padding:24px 0;">
                        <tr>
                          <td align="center">
                            <table role="presentation" width="600" cellspacing="0" cellpadding="0" style="max-width:600px;background:#ffffff;border-radius:12px;overflow:hidden;border:1px solid #e5e7eb;">
                              <tr>
                                <td style="background:#0f766e;padding:24px 32px;color:#ffffff;">
                                  <div style="font-size:14px;letter-spacing:1px;text-transform:uppercase;opacity:.85;">Clinic Management System</div>
                                  <h1 style="margin:8px 0 0;font-size:24px;line-height:1.3;">Doctor invitation</h1>
                                </td>
                              </tr>
                              <tr>
                                <td style="padding:32px;">
                                  <p style="margin:0 0 16px;font-size:16px;">Hello %s,</p>
                                  <p style="margin:0 0 16px;font-size:16px;line-height:1.6;">
                                    You have been invited to join <strong>%s</strong> as a doctor.
                                    Complete your onboarding by setting your password and building your professional profile.
                                  </p>
                                  <p style="margin:24px 0;text-align:center;">
                                    <a href="%s" style="display:inline-block;background:#0f766e;color:#ffffff;text-decoration:none;padding:14px 28px;border-radius:8px;font-weight:bold;font-size:16px;">Complete onboarding</a>
                                  </p>
                                  <p style="margin:0 0 8px;font-size:13px;color:#6b7280;">If the button does not work, open this link:</p>
                                  <p style="margin:0 0 20px;font-size:12px;word-break:break-all;color:#0f766e;">%s</p>
                                  <div style="background:#f9fafb;border:1px solid #e5e7eb;border-radius:8px;padding:16px;">
                                    <p style="margin:0 0 6px;font-size:13px;color:#6b7280;">Or enter this claim token in the app:</p>
                                    <p style="margin:0;font-size:14px;font-family:Consolas,monospace;word-break:break-all;">%s</p>
                                  </div>
                                </td>
                              </tr>
                              <tr>
                                <td style="padding:20px 32px;background:#f9fafb;color:#6b7280;font-size:12px;line-height:1.5;">
                                  This email was sent because this address was added to a doctor invitation for %s.
                                  If you were not expecting it, you can safely ignore this email.
                                  For security, do not share this link or token with anyone.
                                </td>
                              </tr>
                            </table>
                          </td>
                        </tr>
                      </table>
                    </body>
                    </html>
                    """.formatted(safeDoctorName, safeClinicName,
                    HtmlUtils.htmlEscape(claimUrl), HtmlUtils.htmlEscape(claimUrl), HtmlUtils.htmlEscape(claimToken),
                    safeClinicName);

            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, StandardCharsets.UTF_8.name());
            setFromAndReplyTo(helper);
            helper.setTo(toEmail);
            helper.setSubject("Invitation to join " + safe(clinicName, "the clinic"));
            helper.setText(plainText, html);
            message.setHeader("Auto-Submitted", "auto-generated");
            message.setHeader("X-Auto-Response-Suppress", "OOF, AutoReply");
            mailSender.send(message);
            log.info("Doctor invitation email sent successfully");
        } catch (MessagingException | UnsupportedEncodingException | RuntimeException exception) {
            // Never include the claim token, email body, or credentials in application logs.
            log.error("Doctor invitation email delivery failed ({})", exception.getClass().getSimpleName(), exception);
        }
    }

    private void setFromAndReplyTo(MimeMessageHelper helper) throws MessagingException, UnsupportedEncodingException {
        if (fromAddress.isBlank()) {
            throw new IllegalStateException("Doctor invitation sender address is not configured");
        }
        if (fromName.isBlank()) {
            helper.setFrom(fromAddress);
        } else {
            helper.setFrom(fromAddress, fromName);
        }
        if (!replyToAddress.isBlank()) {
            helper.setReplyTo(replyToAddress);
        }
    }

    private void warnIfClaimUrlIsNotPublic(String claimUrl) {
        String normalized = claimUrl.toLowerCase(Locale.ROOT);
        if (normalized.contains("localhost") || normalized.contains("127.0.0.1")
                || normalized.startsWith("http://")) {
            log.warn("Doctor invitation claim URL is not a public HTTPS URL; "
                    + "this hurts deliverability and may be unusable on the recipient device. "
                    + "Set DOCTOR_INVITATION_CLAIM_PAGE_URL to the deployed HTTPS claim page.");
        }
    }

    private static String safe(String value, String fallback) {
        return value == null || value.isBlank() ? fallback : value.trim();
    }
}
