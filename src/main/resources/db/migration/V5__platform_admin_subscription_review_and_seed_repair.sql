-- V5: platform admin grants, admin audit, catalog/admin support, subscription, review, and safe Arabic seed repair.
-- Do NOT modify V1-V4 (already applied).

-- Safe repair of Arabic seed text inserted by V2 (idempotent by stable IDs).
UPDATE clinic.area SET name_ar = 'الدقي' WHERE id = '20000000-0000-0000-0000-000000000001';
UPDATE clinic.area SET name_ar = 'المهندسين' WHERE id = '20000000-0000-0000-0000-000000000002';
UPDATE clinic.area SET name_ar = 'الهرم' WHERE id = '20000000-0000-0000-0000-000000000003';
UPDATE clinic.area SET name_ar = 'الشيخ زايد' WHERE id = '20000000-0000-0000-0000-000000000004';
UPDATE clinic.area SET name_ar = 'مدينة نصر' WHERE id = '20000000-0000-0000-0000-000000000005';
UPDATE clinic.area SET name_ar = 'المعادي' WHERE id = '20000000-0000-0000-0000-000000000006';
UPDATE clinic.area SET name_ar = 'مصر الجديدة' WHERE id = '20000000-0000-0000-0000-000000000007';
UPDATE clinic.area SET name_ar = 'العاصمة الإدارية' WHERE id = '20000000-0000-0000-0000-000000000008';
UPDATE clinic.area SET name_ar = 'سموحة' WHERE id = '20000000-0000-0000-0000-000000000009';
UPDATE clinic.area SET name_ar = 'ميامي' WHERE id = '20000000-0000-0000-0000-000000000010';
UPDATE clinic.area SET name_ar = 'ستانلي' WHERE id = '20000000-0000-0000-0000-000000000011';
UPDATE clinic.area SET name_ar = 'المندرة' WHERE id = '20000000-0000-0000-0000-000000000012';

UPDATE doctor.specialty SET name_ar = 'جلدية' WHERE id = '10000000-0000-0000-0000-000000000001';
UPDATE doctor.specialty SET name_ar = 'أطفال' WHERE id = '10000000-0000-0000-0000-000000000002';
UPDATE doctor.specialty SET name_ar = 'قلب' WHERE id = '10000000-0000-0000-0000-000000000003';
UPDATE doctor.specialty SET name_ar = 'نساء وتوليد' WHERE id = '10000000-0000-0000-0000-000000000004';
UPDATE doctor.specialty SET name_ar = 'عظام' WHERE id = '10000000-0000-0000-0000-000000000005';
UPDATE doctor.specialty SET name_ar = 'عيون' WHERE id = '10000000-0000-0000-0000-000000000006';
UPDATE doctor.specialty SET name_ar = 'أسنان' WHERE id = '10000000-0000-0000-0000-000000000007';
UPDATE doctor.specialty SET name_ar = 'باطنة' WHERE id = '10000000-0000-0000-0000-000000000008';
UPDATE doctor.specialty SET name_ar = 'مخ وأعصاب' WHERE id = '10000000-0000-0000-0000-000000000009';
UPDATE doctor.specialty SET name_ar = 'طب نفسي' WHERE id = '10000000-0000-0000-0000-000000000010';
UPDATE doctor.specialty SET name_ar = 'مسالك بولية' WHERE id = '10000000-0000-0000-0000-000000000011';
UPDATE doctor.specialty SET name_ar = 'جراحة عامة' WHERE id = '10000000-0000-0000-0000-000000000012';

CREATE SCHEMA IF NOT EXISTS platform_admin;

CREATE TABLE platform_admin.admin_grant
(
    id            UUID         NOT NULL PRIMARY KEY,
    user_id       UUID         NOT NULL,
    status        VARCHAR(20)  NOT NULL,
    granted_by    UUID,
    revoked_by    UUID,
    revoked_at    TIMESTAMPTZ,
    created_at    TIMESTAMPTZ  NOT NULL DEFAULT now(),
    updated_at    TIMESTAMPTZ  NOT NULL DEFAULT now(),
    version       BIGINT       NOT NULL DEFAULT 0,
    CONSTRAINT uk_platform_admin_grant_user UNIQUE (user_id),
    CONSTRAINT ck_platform_admin_grant_status CHECK (status IN ('ACTIVE','REVOKED'))
);
CREATE INDEX ix_platform_admin_grant_status ON platform_admin.admin_grant (status);

CREATE TABLE platform_admin.admin_grant_permission
(
    grant_id   UUID        NOT NULL,
    permission VARCHAR(64) NOT NULL,
    CONSTRAINT pk_platform_admin_grant_permission PRIMARY KEY (grant_id, permission),
    CONSTRAINT fk_platform_admin_grant_permission FOREIGN KEY (grant_id)
        REFERENCES platform_admin.admin_grant (id) ON DELETE CASCADE
);

CREATE TABLE platform_admin.admin_action_audit
(
    id             UUID         NOT NULL PRIMARY KEY,
    actor_user_id  UUID         NOT NULL,
    action_type    VARCHAR(80)  NOT NULL,
    target_type    VARCHAR(80)  NOT NULL,
    target_id      VARCHAR(120),
    previous_value VARCHAR(500),
    new_value      VARCHAR(500),
    reason         VARCHAR(1000),
    created_at     TIMESTAMPTZ  NOT NULL DEFAULT now()
);
CREATE INDEX ix_platform_admin_audit_actor ON platform_admin.admin_action_audit (actor_user_id, created_at DESC);
CREATE INDEX ix_platform_admin_audit_target ON platform_admin.admin_action_audit (target_type, target_id, created_at DESC);

CREATE SCHEMA IF NOT EXISTS subscription;

CREATE TABLE subscription.plan
(
    id              UUID          NOT NULL PRIMARY KEY,
    code            VARCHAR(60)   NOT NULL,
    name            VARCHAR(120)  NOT NULL,
    duration_days   INTEGER       NOT NULL,
    price_amount    NUMERIC(12,2) NOT NULL,
    currency        VARCHAR(8)    NOT NULL DEFAULT 'EGP',
    clinic_limit    INTEGER,
    doctor_limit    INTEGER,
    active          BOOLEAN       NOT NULL DEFAULT TRUE,
    created_at      TIMESTAMPTZ   NOT NULL DEFAULT now(),
    updated_at      TIMESTAMPTZ   NOT NULL DEFAULT now(),
    version         BIGINT        NOT NULL DEFAULT 0,
    CONSTRAINT uk_subscription_plan_code UNIQUE (code),
    CONSTRAINT ck_subscription_plan_duration CHECK (duration_days > 0),
    CONSTRAINT ck_subscription_plan_price CHECK (price_amount >= 0)
);

CREATE TABLE subscription.clinic_subscription
(
    id           UUID         NOT NULL PRIMARY KEY,
    clinic_id    UUID         NOT NULL,
    plan_id      UUID         NOT NULL,
    status       VARCHAR(20)  NOT NULL,
    starts_at    TIMESTAMPTZ,
    ends_at      TIMESTAMPTZ,
    activated_by UUID,
    cancelled_by UUID,
    reason       VARCHAR(500),
    created_at   TIMESTAMPTZ  NOT NULL DEFAULT now(),
    updated_at   TIMESTAMPTZ  NOT NULL DEFAULT now(),
    version      BIGINT       NOT NULL DEFAULT 0,
    CONSTRAINT fk_subscription_plan FOREIGN KEY (plan_id) REFERENCES subscription.plan (id) ON DELETE RESTRICT,
    CONSTRAINT ck_subscription_status CHECK (status IN ('PENDING','ACTIVE','EXPIRED','SUSPENDED','CANCELLED'))
);
CREATE INDEX ix_subscription_clinic ON subscription.clinic_subscription (clinic_id, status, ends_at);
CREATE INDEX ix_subscription_status_end ON subscription.clinic_subscription (status, ends_at);
CREATE UNIQUE INDEX uk_subscription_active_clinic ON subscription.clinic_subscription (clinic_id) WHERE status = 'ACTIVE';

CREATE SCHEMA IF NOT EXISTS review;

CREATE TABLE review.clinic_review
(
    id               UUID         NOT NULL PRIMARY KEY,
    clinic_id        UUID         NOT NULL,
    doctor_id        UUID,
    patient_user_id  UUID         NOT NULL,
    rating           INTEGER      NOT NULL,
    comment          VARCHAR(2000),
    status           VARCHAR(20)  NOT NULL,
    moderation_reason VARCHAR(1000),
    moderated_by     UUID,
    moderated_at     TIMESTAMPTZ,
    created_at       TIMESTAMPTZ  NOT NULL DEFAULT now(),
    updated_at       TIMESTAMPTZ  NOT NULL DEFAULT now(),
    version          BIGINT       NOT NULL DEFAULT 0,
    CONSTRAINT ck_review_rating CHECK (rating BETWEEN 1 AND 5),
    CONSTRAINT ck_review_status CHECK (status IN ('PENDING','APPROVED','REJECTED','HIDDEN'))
);
CREATE INDEX ix_review_status ON review.clinic_review (status, created_at DESC);
CREATE INDEX ix_review_clinic ON review.clinic_review (clinic_id, status);

CREATE TABLE review.review_moderation_audit
(
    id             UUID         NOT NULL PRIMARY KEY,
    review_id      UUID         NOT NULL,
    actor_user_id  UUID         NOT NULL,
    action_type    VARCHAR(40)  NOT NULL,
    previous_status VARCHAR(20),
    new_status     VARCHAR(20),
    reason         VARCHAR(1000),
    created_at     TIMESTAMPTZ  NOT NULL DEFAULT now(),
    CONSTRAINT fk_review_moderation_review FOREIGN KEY (review_id) REFERENCES review.clinic_review (id) ON DELETE CASCADE
);
CREATE INDEX ix_review_moderation_review ON review.review_moderation_audit (review_id, created_at DESC);
