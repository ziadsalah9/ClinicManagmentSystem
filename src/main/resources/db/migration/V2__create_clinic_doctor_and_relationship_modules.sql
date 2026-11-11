-- Clinic module: normalized area catalogue and clinic profile.
CREATE SCHEMA IF NOT EXISTS clinic;

CREATE TABLE clinic.area
(
    id         UUID         NOT NULL PRIMARY KEY,
    code       VARCHAR(60)  NOT NULL,
    name_en    VARCHAR(120) NOT NULL,
    name_ar    VARCHAR(120) NOT NULL,
    city       VARCHAR(100) NOT NULL,
    active     BOOLEAN      NOT NULL DEFAULT TRUE,
    created_at TIMESTAMPTZ  NOT NULL DEFAULT now(),
    updated_at TIMESTAMPTZ  NOT NULL DEFAULT now(),
    version    BIGINT       NOT NULL DEFAULT 0,
    CONSTRAINT uk_clinic_area_code UNIQUE (code)
);
CREATE INDEX ix_clinic_area_city_active ON clinic.area (city, active, name_en);

INSERT INTO clinic.area (id, code, name_en, name_ar, city) VALUES
('20000000-0000-0000-0000-000000000001', 'DOKKI', 'Dokki', 'الدقي', 'Giza'),
('20000000-0000-0000-0000-000000000002', 'MOHANDESSEEN', 'Mohandesseen', 'المهندسين', 'Giza'),
('20000000-0000-0000-0000-000000000003', 'HARAM', 'Haram', 'الهرم', 'Giza'),
('20000000-0000-0000-0000-000000000004', 'SHEIKH_ZAYED', 'Sheikh Zayed', 'الشيخ زايد', 'Giza'),
('20000000-0000-0000-0000-000000000005', 'NASR_CITY', 'Nasr City', 'مدينة نصر', 'Cairo'),
('20000000-0000-0000-0000-000000000006', 'MAADI', 'Maadi', 'المعادي', 'Cairo'),
('20000000-0000-0000-0000-000000000007', 'HELIOPOLIS', 'Heliopolis', 'مصر الجديدة', 'Cairo'),
('20000000-0000-0000-0000-000000000008', 'NEW_CAPITAL', 'New Capital', 'العاصمة الإدارية', 'New Administrative Capital'),
('20000000-0000-0000-0000-000000000009', 'SMOUHA', 'Smouha', 'سموحة', 'Alexandria'),
('20000000-0000-0000-0000-000000000010', 'MIAMI_ALEX', 'Miami', 'ميامي', 'Alexandria'),
('20000000-0000-0000-0000-000000000011', 'STANLEY', 'Stanley', 'ستانلي', 'Alexandria'),
('20000000-0000-0000-0000-000000000012', 'MANDARA', 'Mandara', 'المندرة', 'Alexandria');

CREATE TABLE clinic.clinic
(
    id             UUID         NOT NULL PRIMARY KEY,
    owner_user_id  UUID         NOT NULL,
    area_id        UUID         NOT NULL,
    name           VARCHAR(150) NOT NULL,
    address        VARCHAR(300) NOT NULL,
    time_zone      VARCHAR(64)  NOT NULL DEFAULT 'Africa/Cairo',
    status         VARCHAR(32)  NOT NULL,
    created_at     TIMESTAMPTZ  NOT NULL DEFAULT now(),
    updated_at     TIMESTAMPTZ  NOT NULL DEFAULT now(),
    version        BIGINT       NOT NULL DEFAULT 0,
    CONSTRAINT fk_clinic_area FOREIGN KEY (area_id)
        REFERENCES clinic.area (id) ON DELETE RESTRICT,
    CONSTRAINT ck_clinic_status CHECK (status IN ('PENDING_VERIFICATION', 'ACTIVE', 'SUSPENDED', 'REJECTED'))
);
CREATE UNIQUE INDEX uk_clinic_name_ci ON clinic.clinic (lower(name));
CREATE INDEX ix_clinic_owner ON clinic.clinic (owner_user_id, created_at DESC);
CREATE INDEX ix_clinic_status_area ON clinic.clinic (status, area_id);

CREATE TABLE clinic.clinic_phone
(
    clinic_id UUID        NOT NULL,
    phone     VARCHAR(16) NOT NULL,
    CONSTRAINT pk_clinic_phone PRIMARY KEY (clinic_id, phone),
    CONSTRAINT fk_clinic_phone_clinic FOREIGN KEY (clinic_id)
        REFERENCES clinic.clinic (id) ON DELETE CASCADE,
    CONSTRAINT ck_clinic_phone_e164 CHECK (phone ~ '^\+[1-9][0-9]{7,14}$')
);

-- Clinic creation idempotency is committed atomically with the clinic and owner membership.
CREATE TABLE clinic.create_idempotency
(
    actor_user_id   UUID         NOT NULL,
    idempotency_key VARCHAR(100) NOT NULL,
    request_hash    CHAR(64)     NOT NULL,
    clinic_id       UUID,
    created_at      TIMESTAMPTZ  NOT NULL DEFAULT now(),
    CONSTRAINT pk_clinic_create_idempotency PRIMARY KEY (actor_user_id, idempotency_key),
    CONSTRAINT fk_clinic_create_idempotency_clinic FOREIGN KEY (clinic_id)
        REFERENCES clinic.clinic (id) ON DELETE CASCADE
);
CREATE INDEX ix_clinic_create_idempotency_created_at ON clinic.create_idempotency (created_at);

-- Doctor module: professional profile and specialty catalogue.
CREATE SCHEMA IF NOT EXISTS doctor;

CREATE TABLE doctor.specialty
(
    id         UUID         NOT NULL PRIMARY KEY,
    code       VARCHAR(50)  NOT NULL,
    name_en    VARCHAR(120) NOT NULL,
    name_ar    VARCHAR(120) NOT NULL,
    active     BOOLEAN      NOT NULL DEFAULT TRUE,
    created_at TIMESTAMPTZ  NOT NULL DEFAULT now(),
    updated_at TIMESTAMPTZ  NOT NULL DEFAULT now(),
    version    BIGINT       NOT NULL DEFAULT 0,
    CONSTRAINT uk_doctor_specialty_code UNIQUE (code)
);

CREATE TABLE doctor.doctor_profile
(
    id                  UUID         NOT NULL PRIMARY KEY,
    user_id             UUID         NOT NULL,
    professional_title  VARCHAR(120) NOT NULL,
    bio                 VARCHAR(2000),
    years_experience    INTEGER      NOT NULL,
    syndicate_number    VARCHAR(50),
    gender              VARCHAR(20)  NOT NULL,
    verification_status VARCHAR(32)  NOT NULL,
    created_at          TIMESTAMPTZ  NOT NULL DEFAULT now(),
    updated_at          TIMESTAMPTZ  NOT NULL DEFAULT now(),
    version             BIGINT       NOT NULL DEFAULT 0,
    CONSTRAINT uk_doctor_profile_user UNIQUE (user_id),
    CONSTRAINT ck_doctor_years_experience CHECK (years_experience BETWEEN 0 AND 80),
    CONSTRAINT ck_doctor_gender CHECK (gender IN ('MALE', 'FEMALE', 'UNSPECIFIED')),
    CONSTRAINT ck_doctor_verification_status CHECK (verification_status IN ('PENDING_VERIFICATION', 'APPROVED', 'NEEDS_INFORMATION', 'REJECTED'))
);
CREATE INDEX ix_doctor_profile_verification ON doctor.doctor_profile (verification_status, created_at DESC);
CREATE UNIQUE INDEX uk_doctor_profile_syndicate_number_ci ON doctor.doctor_profile (lower(syndicate_number))
    WHERE syndicate_number IS NOT NULL;

CREATE TABLE doctor.doctor_specialty
(
    doctor_id    UUID NOT NULL,
    specialty_id UUID NOT NULL,
    CONSTRAINT pk_doctor_specialty PRIMARY KEY (doctor_id, specialty_id),
    CONSTRAINT fk_doctor_specialty_doctor FOREIGN KEY (doctor_id)
        REFERENCES doctor.doctor_profile (id) ON DELETE CASCADE,
    CONSTRAINT fk_doctor_specialty_specialty FOREIGN KEY (specialty_id)
        REFERENCES doctor.specialty (id) ON DELETE RESTRICT
);
CREATE INDEX ix_doctor_specialty_specialty ON doctor.doctor_specialty (specialty_id, doctor_id);

INSERT INTO doctor.specialty (id, code, name_en, name_ar) VALUES
('10000000-0000-0000-0000-000000000001', 'DERMATOLOGY', 'Dermatology', 'جلدية'),
('10000000-0000-0000-0000-000000000002', 'PEDIATRICS', 'Pediatrics', 'أطفال'),
('10000000-0000-0000-0000-000000000003', 'CARDIOLOGY', 'Cardiology', 'قلب'),
('10000000-0000-0000-0000-000000000004', 'GYNECOLOGY', 'Gynecology', 'نساء وتوليد'),
('10000000-0000-0000-0000-000000000005', 'ORTHOPEDICS', 'Orthopedics', 'عظام'),
('10000000-0000-0000-0000-000000000006', 'OPHTHALMOLOGY', 'Ophthalmology', 'عيون'),
('10000000-0000-0000-0000-000000000007', 'DENTISTRY', 'Dentistry', 'أسنان'),
('10000000-0000-0000-0000-000000000008', 'INTERNAL_MEDICINE', 'Internal Medicine', 'باطنة'),
('10000000-0000-0000-0000-000000000009', 'NEUROLOGY', 'Neurology', 'مخ وأعصاب'),
('10000000-0000-0000-0000-000000000010', 'PSYCHIATRY', 'Psychiatry', 'طب نفسي'),
('10000000-0000-0000-0000-000000000011', 'UROLOGY', 'Urology', 'مسالك بولية'),
('10000000-0000-0000-0000-000000000012', 'GENERAL_SURGERY', 'General Surgery', 'جراحة عامة');

-- DoctorClinic owns per-clinic configuration and pricing for a Doctor at a specific Clinic.
-- Cross-module IDs are validated through public Facades; no cross-module foreign keys.
CREATE SCHEMA IF NOT EXISTS doctor_clinic;

CREATE TABLE doctor_clinic.doctor_clinic
(
    id                       UUID          NOT NULL PRIMARY KEY,
    clinic_id                UUID          NOT NULL,
    doctor_id                UUID          NOT NULL,
    consultation_fee         NUMERIC(12,2) NOT NULL,
    follow_up_fee            NUMERIC(12,2) NOT NULL,
    follow_up_validity_days  INTEGER       NOT NULL DEFAULT 14,
    instapay_address         VARCHAR(150),
    auto_confirm             BOOLEAN       NOT NULL DEFAULT TRUE,
    status                   VARCHAR(20)   NOT NULL DEFAULT 'ACTIVE',
    created_at               TIMESTAMPTZ   NOT NULL DEFAULT now(),
    updated_at               TIMESTAMPTZ   NOT NULL DEFAULT now(),
    version                  BIGINT        NOT NULL DEFAULT 0,
    CONSTRAINT uk_doctor_clinic_pair UNIQUE (clinic_id, doctor_id),
    CONSTRAINT ck_doctor_clinic_consultation_fee CHECK (consultation_fee >= 0),
    CONSTRAINT ck_doctor_clinic_follow_up_fee CHECK (follow_up_fee >= 0),
    CONSTRAINT ck_doctor_clinic_follow_up_validity CHECK (follow_up_validity_days BETWEEN 1 AND 365),
    CONSTRAINT ck_doctor_clinic_status CHECK (status IN ('ACTIVE', 'SUSPENDED', 'ENDED'))
);
CREATE INDEX ix_doctor_clinic_clinic ON doctor_clinic.doctor_clinic (clinic_id, status, doctor_id);
CREATE INDEX ix_doctor_clinic_doctor ON doctor_clinic.doctor_clinic (doctor_id, status, clinic_id);
