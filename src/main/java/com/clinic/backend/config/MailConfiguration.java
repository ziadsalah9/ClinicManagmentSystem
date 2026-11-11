package com.clinic.backend.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.mail.javamail.JavaMailSenderImpl;

/**
 * Gmail displays app passwords in groups separated by spaces. SMTP expects the
 * password without those spaces, so normalize the configured value once at startup.
 */
@Configuration(proxyBeanMethods = false)
public class MailConfiguration {
    private static final Logger log = LoggerFactory.getLogger(MailConfiguration.class);

    MailConfiguration(JavaMailSenderImpl mailSender,
                      @Value("${spring.mail.host:}") String host,
                      @Value("${spring.mail.password:}") String password) {
        if (!"smtp.gmail.com".equalsIgnoreCase(host) || password == null || password.isBlank()) {
            return;
        }

        String normalized = password.trim();
        if ((normalized.startsWith("\"") && normalized.endsWith("\""))
                || (normalized.startsWith("'") && normalized.endsWith("'"))) {
            normalized = normalized.substring(1, normalized.length() - 1);
        }
        normalized = normalized.replaceAll("\\s+", "");

        if (!normalized.equals(password)) {
            mailSender.setPassword(normalized);
            log.info("Normalized Gmail app password whitespace/quotes for SMTP authentication");
        }
    }
}
