CREATE SCHEMA IF NOT EXISTS identity;

CREATE TABLE identity.app_user
(
    id            UUID         NOT NULL PRIMARY KEY,
    phone         VARCHAR(16)  NOT NULL,
    full_name     VARCHAR(150) NOT NULL,
    email         VARCHAR(254),
    password_hash VARCHAR(100),
    status        VARCHAR(32)  NOT NULL,
    locale        VARCHAR(8)   NOT NULL DEFAULT 'ar',
    created_at    TIMESTAMPTZ  NOT NULL DEFAULT now(),
    updated_at    TIMESTAMPTZ  NOT NULL DEFAULT now(),
    version       BIGINT       NOT NULL DEFAULT 0,
    CONSTRAINT uk_identity_user_phone UNIQUE (phone),
    CONSTRAINT ck_identity_user_phone_e164 CHECK (phone ~ '^\+[1-9][0-9]{7,14}$'),
    CONSTRAINT ck_identity_user_status CHECK (status IN ('ACTIVE', 'SUSPENDED', 'PENDING_VERIFICATION'))
);

CREATE UNIQUE INDEX uk_identity_user_email_not_null
    ON identity.app_user (lower(email))
    WHERE email IS NOT NULL;

CREATE TABLE identity.staff_membership
(
    id         UUID        NOT NULL PRIMARY KEY,
    user_id    UUID        NOT NULL,
    clinic_id  UUID        NOT NULL,
    role       VARCHAR(32) NOT NULL,
    status     VARCHAR(32) NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    version    BIGINT      NOT NULL DEFAULT 0,
    CONSTRAINT fk_identity_membership_user FOREIGN KEY (user_id)
        REFERENCES identity.app_user (id) ON DELETE RESTRICT,
    CONSTRAINT uk_identity_membership_clinic_user UNIQUE (clinic_id, user_id),
    CONSTRAINT ck_identity_membership_role CHECK (role IN ('OWNER', 'DOCTOR', 'SECRETARY', 'ACCOUNTANT')),
    CONSTRAINT ck_identity_membership_status CHECK (status IN ('ACTIVE', 'SUSPENDED', 'REVOKED'))
);

CREATE INDEX ix_identity_membership_user ON identity.staff_membership (user_id, status);
CREATE INDEX ix_identity_membership_clinic ON identity.staff_membership (clinic_id, status, role);

CREATE TABLE identity.membership_permission
(
    membership_id UUID        NOT NULL,
    permission    VARCHAR(64) NOT NULL,
    CONSTRAINT pk_identity_membership_permission PRIMARY KEY (membership_id, permission),
    CONSTRAINT fk_identity_membership_permission_membership FOREIGN KEY (membership_id)
        REFERENCES identity.staff_membership (id) ON DELETE CASCADE
);

CREATE TABLE identity.staff_invitation
(
    id                  UUID         NOT NULL PRIMARY KEY,
    clinic_id           UUID         NOT NULL,
    phone               VARCHAR(16)  NOT NULL,
    role                VARCHAR(32)  NOT NULL,
    token_hash          VARCHAR(64)  NOT NULL,
    invited_by_user_id  UUID         NOT NULL,
    status              VARCHAR(32)  NOT NULL,
    expires_at          TIMESTAMPTZ  NOT NULL,
    accepted_at         TIMESTAMPTZ,
    created_at          TIMESTAMPTZ  NOT NULL DEFAULT now(),
    updated_at          TIMESTAMPTZ  NOT NULL DEFAULT now(),
    version             BIGINT       NOT NULL DEFAULT 0,
    CONSTRAINT fk_identity_invitation_inviter FOREIGN KEY (invited_by_user_id)
        REFERENCES identity.app_user (id) ON DELETE RESTRICT,
    CONSTRAINT uk_identity_invitation_token_hash UNIQUE (token_hash),
    CONSTRAINT ck_identity_invitation_role CHECK (role IN ('DOCTOR', 'SECRETARY', 'ACCOUNTANT')),
    CONSTRAINT ck_identity_invitation_status CHECK (status IN ('PENDING', 'ACCEPTED', 'REVOKED', 'EXPIRED'))
);

CREATE UNIQUE INDEX uk_identity_pending_invitation_clinic_phone
    ON identity.staff_invitation (clinic_id, phone)
    WHERE status = 'PENDING';

CREATE INDEX ix_identity_invitation_expiry ON identity.staff_invitation (expires_at, status);

CREATE TABLE identity.invitation_permission
(
    invitation_id UUID        NOT NULL,
    permission    VARCHAR(64) NOT NULL,
    CONSTRAINT pk_identity_invitation_permission PRIMARY KEY (invitation_id, permission),
    CONSTRAINT fk_identity_invitation_permission_invitation FOREIGN KEY (invitation_id)
        REFERENCES identity.staff_invitation (id) ON DELETE CASCADE
);

CREATE TABLE identity.otp_challenge
(
    id                         UUID        NOT NULL PRIMARY KEY,
    phone                      VARCHAR(16) NOT NULL,
    purpose                    VARCHAR(32) NOT NULL,
    code_hash                  VARCHAR(100) NOT NULL,
    attempt_count              INTEGER     NOT NULL DEFAULT 0,
    expires_at                 TIMESTAMPTZ NOT NULL,
    verified_token_hash        VARCHAR(64),
    verified_token_expires_at  TIMESTAMPTZ,
    consumed_at                TIMESTAMPTZ,
    created_at                 TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at                 TIMESTAMPTZ NOT NULL DEFAULT now(),
    version                    BIGINT      NOT NULL DEFAULT 0,
    CONSTRAINT uk_identity_otp_verification_token UNIQUE (verified_token_hash),
    CONSTRAINT ck_identity_otp_purpose CHECK (purpose IN ('OWNER_REGISTRATION', 'LOGIN')),
    CONSTRAINT ck_identity_otp_attempts CHECK (attempt_count BETWEEN 0 AND 10)
);

CREATE INDEX ix_identity_otp_phone_created ON identity.otp_challenge (phone, created_at DESC);
CREATE INDEX ix_identity_otp_expiry ON identity.otp_challenge (expires_at);

CREATE TABLE identity.refresh_session
(
    id                       UUID         NOT NULL PRIMARY KEY,
    user_id                  UUID         NOT NULL,
    token_hash               VARCHAR(64)  NOT NULL,
    expires_at               TIMESTAMPTZ  NOT NULL,
    revoked_at               TIMESTAMPTZ,
    replaced_by_session_id   UUID,
    device_id                VARCHAR(128),
    created_at               TIMESTAMPTZ  NOT NULL DEFAULT now(),
    updated_at               TIMESTAMPTZ  NOT NULL DEFAULT now(),
    version                  BIGINT       NOT NULL DEFAULT 0,
    CONSTRAINT fk_identity_refresh_user FOREIGN KEY (user_id)
        REFERENCES identity.app_user (id) ON DELETE CASCADE,
    CONSTRAINT uk_identity_refresh_token_hash UNIQUE (token_hash),
    CONSTRAINT fk_identity_refresh_replacement FOREIGN KEY (replaced_by_session_id)
        REFERENCES identity.refresh_session (id) ON DELETE SET NULL
);

CREATE INDEX ix_identity_refresh_user_active ON identity.refresh_session (user_id, expires_at)
    WHERE revoked_at IS NULL;


CREATE TABLE identity.password_login_attempt
(
    phone             VARCHAR(16) NOT NULL PRIMARY KEY,
    failed_attempts   INTEGER     NOT NULL DEFAULT 0,
    window_started_at TIMESTAMPTZ NOT NULL,
    locked_until      TIMESTAMPTZ,
    created_at        TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at        TIMESTAMPTZ NOT NULL DEFAULT now(),
    version           BIGINT      NOT NULL DEFAULT 0,
    CONSTRAINT ck_identity_password_failures CHECK (failed_attempts BETWEEN 0 AND 5)
);
