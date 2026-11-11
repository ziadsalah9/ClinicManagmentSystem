package com.clinic.backend.identity.internal.infrastructure.persistence;

import com.clinic.backend.shared.AuditedJpaEntity;

import com.clinic.backend.identity.internal.domain.SystemRole;
import com.clinic.backend.identity.internal.domain.UserStatus;
import jakarta.persistence.*;

import java.util.UUID;

@Entity
@Table(name = "app_user", schema = "identity")
public class AppUserJpaEntity extends AuditedJpaEntity {
    @Id
    private UUID id;

    @Column(name = "phone", nullable = false, length = 16, unique = true)
    private String phone;

    @Column(name = "full_name", nullable = false, length = 150)
    private String fullName;

    @Column(name = "email", length = 254, unique = true)
    private String email;

    @Column(name = "password_hash", length = 100)
    private String passwordHash;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 32)
    private UserStatus status;

    @Column(name = "locale", nullable = false, length = 8)
    private String locale;

    @Enumerated(EnumType.STRING)
    @Column(name = "system_role", nullable = false, length = 16)
    private SystemRole systemRole = SystemRole.USER;

    @Column(name = "must_change_password", nullable = false)
    private boolean mustChangePassword;

    protected AppUserJpaEntity() { }

    public AppUserJpaEntity(UUID id, String phone, String fullName, String email,
                            String passwordHash, UserStatus status, String locale, SystemRole systemRole,
                            boolean mustChangePassword) {
        this.id = id;
        this.phone = phone;
        this.fullName = fullName;
        this.email = email;
        this.passwordHash = passwordHash;
        this.status = status;
        this.locale = locale;
        this.systemRole = systemRole == null ? SystemRole.USER : systemRole;
        this.mustChangePassword = mustChangePassword;
    }

    public void apply(com.clinic.backend.identity.internal.domain.AppUser user) {
        this.fullName = user.fullName();
        this.email = user.email();
        this.passwordHash = user.passwordHash();
        this.status = user.status();
        this.locale = user.locale();
        this.systemRole = user.systemRole() == null ? SystemRole.USER : user.systemRole();
        this.mustChangePassword = user.mustChangePassword();
    }

    public UUID getId() { return id; }
    public String getPhone() { return phone; }
    public String getFullName() { return fullName; }
    public String getEmail() { return email; }
    public String getPasswordHash() { return passwordHash; }
    public UserStatus getStatus() { return status; }
    public String getLocale() { return locale; }
    public SystemRole getSystemRole() { return systemRole; }
    public boolean isMustChangePassword() { return mustChangePassword; }
}
