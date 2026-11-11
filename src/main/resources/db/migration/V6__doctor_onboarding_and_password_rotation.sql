-- V6: doctor onboarding (invite/claim + forced password rotation)
ALTER TABLE identity.app_user
    ADD COLUMN IF NOT EXISTS must_change_password BOOLEAN NOT NULL DEFAULT FALSE;

CREATE TABLE IF NOT EXISTS identity.doctor_onboarding_invite
(
    id                  UUID          NOT NULL PRIMARY KEY,
    clinic_id           UUID          NOT NULL,
    phone               VARCHAR(16)   NOT NULL,
    full_name           VARCHAR(150)  NOT NULL,
    email               VARCHAR(254),
    professional_title  VARCHAR(120),
    bio                 VARCHAR(2000),
    years_experience    INTEGER,
    syndicate_number    VARCHAR(50),
    gender              VARCHAR(20),
    specialty_ids       TEXT,
    token_hash          VARCHAR(64)   NOT NULL,
    invited_by_user_id  UUID          NOT NULL,
    status              VARCHAR(32)   NOT NULL,
    attempt_count       INTEGER       NOT NULL DEFAULT 0,
    expires_at          TIMESTAMPTZ   NOT NULL,
    claimed_at          TIMESTAMPTZ,
    claimed_user_id     UUID,
    created_at          TIMESTAMPTZ   NOT NULL DEFAULT now(),
    updated_at          TIMESTAMPTZ   NOT NULL DEFAULT now(),
    version             BIGINT        NOT NULL DEFAULT 0,
    CONSTRAINT uk_identity_doc_invite_token_hash UNIQUE (token_hash),
    CONSTRAINT ck_identity_doc_invite_status CHECK (status IN ('PENDING','CLAIMED','EXPIRED','REVOKED')),
    CONSTRAINT ck_identity_doc_invite_attempts CHECK (attempt_count BETWEEN 0 AND 10),
    CONSTRAINT ck_identity_doc_invite_gender CHECK (gender IS NULL OR gender IN ('MALE','FEMALE','UNSPECIFIED'))
);

CREATE INDEX IF NOT EXISTS ix_identity_doc_invite_phone_created ON identity.doctor_onboarding_invite (phone, created_at DESC);
CREATE INDEX IF NOT EXISTS ix_identity_doc_invite_expiry ON identity.doctor_onboarding_invite (status, expires_at);
CREATE INDEX IF NOT EXISTS ix_identity_doc_invite_clinic_phone ON identity.doctor_onboarding_invite (clinic_id, phone, status);
