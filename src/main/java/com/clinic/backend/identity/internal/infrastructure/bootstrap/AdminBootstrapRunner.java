package com.clinic.backend.identity.internal.infrastructure.bootstrap;

import com.clinic.backend.identity.internal.application.port.IdentityStore;
import com.clinic.backend.identity.internal.domain.AppUser;
import com.clinic.backend.identity.internal.domain.SystemRole;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.regex.Pattern;

@Component
public class AdminBootstrapRunner implements ApplicationRunner {
    private static final Logger log = LoggerFactory.getLogger(AdminBootstrapRunner.class);
    private static final Pattern E164 = Pattern.compile("^\\+[1-9]\\d{7,14}$");

    private final IdentityStore store;
    private final PasswordEncoder passwordEncoder;
    private final String adminPhone;
    private final String adminPassword;
    private final String adminFullName;
    private final String adminEmail;

    public AdminBootstrapRunner(
            IdentityStore store,
            PasswordEncoder passwordEncoder,
            @Value("${identity.bootstrap.admin.phone:}") String adminPhone,
            @Value("${identity.bootstrap.admin.password:}") String adminPassword,
            @Value("${identity.bootstrap.admin.full-name:System Admin}") String adminFullName,
            @Value("${identity.bootstrap.admin.email:}") String adminEmail
    ) {
        this.store = store;
        this.passwordEncoder = passwordEncoder;
        this.adminPhone = adminPhone == null ? "" : adminPhone.trim();
        this.adminPassword = adminPassword == null ? "" : adminPassword;
        this.adminFullName = adminFullName == null || adminFullName.isBlank() ? "System Admin" : adminFullName.trim();
        this.adminEmail = adminEmail == null || adminEmail.isBlank() ? null : adminEmail.trim().toLowerCase();
    }

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        if (adminPhone.isBlank() || adminPassword.isBlank()) {
            return; // opt-in bootstrap only
        }
        if (!E164.matcher(adminPhone).matches()) {
            log.warn("Skipping admin bootstrap: identity.bootstrap.admin.phone must be E.164");
            return;
        }
        if (adminPassword.length() < 10 || adminPassword.length() > 64) {
            log.warn("Skipping admin bootstrap: identity.bootstrap.admin.password must be 10-64 chars");
            return;
        }
        store.lockUserPhone(adminPhone);
        if (store.findUserByPhone(adminPhone).isPresent()) {
            log.info("Admin bootstrap skipped: user already exists for {}", adminPhone);
            return;
        }
        AppUser admin = AppUser.provisioned(adminPhone, adminFullName, adminEmail,
                passwordEncoder.encode(adminPassword), "ar", SystemRole.ADMIN, false);
        store.saveUser(admin);
        log.info("Bootstrapped system admin account for {}", adminPhone);
    }
}