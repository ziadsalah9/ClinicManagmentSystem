-- Clinic verification workflow audit trail.
-- Every status-changing decision and information request records the administrator,
-- action, previous/new status, reason, and timestamp in the same transaction as the change.
CREATE TABLE clinic.verification_event
(
    id              UUID         NOT NULL PRIMARY KEY,
    clinic_id       UUID         NOT NULL,
    actor_user_id   UUID         NOT NULL,
    action          VARCHAR(40)  NOT NULL,
    previous_status VARCHAR(32)  NOT NULL,
    new_status      VARCHAR(32)  NOT NULL,
    reason          VARCHAR(1000),
    created_at      TIMESTAMPTZ  NOT NULL DEFAULT now(),
    CONSTRAINT fk_clinic_verification_event_clinic FOREIGN KEY (clinic_id)
        REFERENCES clinic.clinic (id) ON DELETE CASCADE,
    CONSTRAINT ck_clinic_verification_action CHECK (
        action IN ('APPROVE', 'REJECT', 'REQUEST_INFORMATION', 'SUSPEND', 'REACTIVATE')
    ),
    CONSTRAINT ck_clinic_verification_previous_status CHECK (
        previous_status IN ('PENDING_VERIFICATION', 'ACTIVE', 'SUSPENDED', 'REJECTED')
    ),
    CONSTRAINT ck_clinic_verification_new_status CHECK (
        new_status IN ('PENDING_VERIFICATION', 'ACTIVE', 'SUSPENDED', 'REJECTED')
    )
);
CREATE INDEX ix_clinic_verification_event_clinic
    ON clinic.verification_event (clinic_id, created_at DESC);
CREATE INDEX ix_clinic_verification_event_actor
    ON clinic.verification_event (actor_user_id, created_at DESC);
