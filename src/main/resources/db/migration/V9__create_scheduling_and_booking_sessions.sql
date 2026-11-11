-- Scheduling owns shift configuration and temporary exceptions.
-- Booking owns generated, bookable sessions. Cross-module identifiers are validated
-- through public facades; this migration intentionally adds no cross-module FKs.
CREATE SCHEMA IF NOT EXISTS scheduling;

CREATE TABLE scheduling.shift
(
    id                     UUID         NOT NULL PRIMARY KEY,
    doctor_clinic_id       UUID         NOT NULL,
    name                   VARCHAR(100) NOT NULL,
    capacity_mode          VARCHAR(20)  NOT NULL,
    average_visit_minutes  INTEGER,
    fixed_capacity         INTEGER,
    effective_from         DATE         NOT NULL,
    active                 BOOLEAN      NOT NULL DEFAULT TRUE,
    created_at             TIMESTAMPTZ  NOT NULL DEFAULT now(),
    updated_at             TIMESTAMPTZ  NOT NULL DEFAULT now(),
    version                BIGINT       NOT NULL DEFAULT 0,
    CONSTRAINT ck_scheduling_shift_mode CHECK (capacity_mode IN ('CALCULATED', 'FIXED')),
    CONSTRAINT ck_scheduling_shift_capacity CHECK (
        (capacity_mode = 'CALCULATED' AND average_visit_minutes BETWEEN 3 AND 60 AND fixed_capacity IS NULL)
        OR (capacity_mode = 'FIXED' AND fixed_capacity > 0 AND average_visit_minutes IS NULL)
    )
);
CREATE UNIQUE INDEX uk_scheduling_shift_name_ci
    ON scheduling.shift (doctor_clinic_id, lower(name));
CREATE INDEX ix_scheduling_shift_doctor_clinic_active
    ON scheduling.shift (doctor_clinic_id, active, effective_from);

CREATE TABLE scheduling.weekly_rule
(
    id              UUID        NOT NULL PRIMARY KEY,
    shift_id        UUID        NOT NULL,
    day_of_week     VARCHAR(12) NOT NULL,
    start_time      TIME        NOT NULL,
    end_time        TIME        NOT NULL,
    follow_up_quota INTEGER     NOT NULL DEFAULT 0,
    CONSTRAINT fk_scheduling_weekly_rule_shift FOREIGN KEY (shift_id)
        REFERENCES scheduling.shift (id) ON DELETE CASCADE,
    CONSTRAINT uk_scheduling_weekly_rule_day UNIQUE (shift_id, day_of_week),
    CONSTRAINT ck_scheduling_weekly_rule_day CHECK (
        day_of_week IN ('MONDAY', 'TUESDAY', 'WEDNESDAY', 'THURSDAY', 'FRIDAY', 'SATURDAY', 'SUNDAY')
    ),
    CONSTRAINT ck_scheduling_weekly_rule_time CHECK (start_time < end_time),
    CONSTRAINT ck_scheduling_weekly_rule_quota CHECK (follow_up_quota >= 0)
);
CREATE INDEX ix_scheduling_weekly_rule_shift ON scheduling.weekly_rule (shift_id);

CREATE TABLE scheduling.shift_break
(
    id         UUID NOT NULL PRIMARY KEY,
    rule_id    UUID NOT NULL,
    start_time TIME NOT NULL,
    end_time   TIME NOT NULL,
    CONSTRAINT fk_scheduling_shift_break_rule FOREIGN KEY (rule_id)
        REFERENCES scheduling.weekly_rule (id) ON DELETE CASCADE,
    CONSTRAINT ck_scheduling_shift_break_time CHECK (start_time < end_time)
);
CREATE INDEX ix_scheduling_shift_break_rule ON scheduling.shift_break (rule_id);

CREATE TABLE scheduling.schedule_exception
(
    id                  UUID         NOT NULL PRIMARY KEY,
    doctor_clinic_id    UUID         NOT NULL,
    shift_id            UUID,
    type                VARCHAR(20)  NOT NULL,
    date_from           DATE         NOT NULL,
    date_to             DATE         NOT NULL,
    changed_start_time  TIME,
    changed_end_time    TIME,
    reason              VARCHAR(500),
    cancelled_at        TIMESTAMPTZ,
    created_at          TIMESTAMPTZ  NOT NULL DEFAULT now(),
    updated_at          TIMESTAMPTZ  NOT NULL DEFAULT now(),
    version             BIGINT       NOT NULL DEFAULT 0,
    CONSTRAINT ck_scheduling_exception_type CHECK (type IN ('DAY_OFF', 'CHANGED_HOURS')),
    CONSTRAINT ck_scheduling_exception_dates CHECK (date_from <= date_to),
    CONSTRAINT ck_scheduling_exception_hours CHECK (
        (type = 'DAY_OFF' AND changed_start_time IS NULL AND changed_end_time IS NULL)
        OR (type = 'CHANGED_HOURS' AND changed_start_time IS NOT NULL
            AND changed_end_time IS NOT NULL AND changed_start_time < changed_end_time)
    )
);
CREATE INDEX ix_scheduling_exception_doctor_clinic_dates
    ON scheduling.schedule_exception (doctor_clinic_id, date_from, date_to);

CREATE TABLE scheduling.schedule_guard
(
    doctor_clinic_id UUID        NOT NULL PRIMARY KEY,
    updated_at       TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE SCHEMA IF NOT EXISTS booking;

CREATE TABLE booking.session
(
    id                       UUID        NOT NULL PRIMARY KEY,
    doctor_clinic_id         UUID        NOT NULL,
    shift_id                 UUID        NOT NULL,
    session_date             DATE        NOT NULL,
    start_time               TIME        NOT NULL,
    end_time                 TIME        NOT NULL,
    capacity                 INTEGER     NOT NULL,
    booked_count             INTEGER     NOT NULL DEFAULT 0,
    follow_up_quota          INTEGER     NOT NULL DEFAULT 0,
    follow_up_booked_count   INTEGER     NOT NULL DEFAULT 0,
    status                   VARCHAR(20) NOT NULL DEFAULT 'ACTIVE',
    created_at               TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at               TIMESTAMPTZ NOT NULL DEFAULT now(),
    version                  BIGINT      NOT NULL DEFAULT 0,
    CONSTRAINT uk_booking_session_slot UNIQUE (doctor_clinic_id, shift_id, session_date),
    CONSTRAINT ck_booking_session_time CHECK (start_time < end_time),
    CONSTRAINT ck_booking_session_capacity CHECK (
        capacity > 0 AND booked_count >= 0 AND booked_count <= capacity
        AND follow_up_quota >= 0 AND follow_up_quota <= capacity
        AND follow_up_booked_count >= 0 AND follow_up_booked_count <= follow_up_quota
    )
);
CREATE INDEX ix_booking_session_doctor_clinic_date
    ON booking.session (doctor_clinic_id, session_date, start_time);
