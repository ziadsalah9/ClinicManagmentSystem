--
-- PostgreSQL database dump
--

\restrict hB0lDd6WJ0aByFB5P9aWSHo4OGa1f0aQwwg8lGwEYmHsK5df7EK0mkzj3ZuqAUl

-- Dumped from database version 16.15
-- Dumped by pg_dump version 16.15

SET statement_timeout = 0;
SET lock_timeout = 0;
SET idle_in_transaction_session_timeout = 0;
SET client_encoding = 'UTF8';
SET standard_conforming_strings = on;
SELECT pg_catalog.set_config('search_path', '', false);
SET check_function_bodies = false;
SET xmloption = content;
SET client_min_messages = warning;
SET row_security = off;

--
-- Name: booking; Type: SCHEMA; Schema: -; Owner: postgres
--

CREATE SCHEMA booking;


ALTER SCHEMA booking OWNER TO postgres;

--
-- Name: clinic; Type: SCHEMA; Schema: -; Owner: postgres
--

CREATE SCHEMA clinic;


ALTER SCHEMA clinic OWNER TO postgres;

--
-- Name: doctor; Type: SCHEMA; Schema: -; Owner: postgres
--

CREATE SCHEMA doctor;


ALTER SCHEMA doctor OWNER TO postgres;

--
-- Name: doctor_clinic; Type: SCHEMA; Schema: -; Owner: postgres
--

CREATE SCHEMA doctor_clinic;


ALTER SCHEMA doctor_clinic OWNER TO postgres;

--
-- Name: identity; Type: SCHEMA; Schema: -; Owner: postgres
--

CREATE SCHEMA identity;


ALTER SCHEMA identity OWNER TO postgres;

--
-- Name: platform_admin; Type: SCHEMA; Schema: -; Owner: postgres
--

CREATE SCHEMA platform_admin;


ALTER SCHEMA platform_admin OWNER TO postgres;

--
-- Name: review; Type: SCHEMA; Schema: -; Owner: postgres
--

CREATE SCHEMA review;


ALTER SCHEMA review OWNER TO postgres;

--
-- Name: scheduling; Type: SCHEMA; Schema: -; Owner: postgres
--

CREATE SCHEMA scheduling;


ALTER SCHEMA scheduling OWNER TO postgres;

--
-- Name: subscription; Type: SCHEMA; Schema: -; Owner: postgres
--

CREATE SCHEMA subscription;


ALTER SCHEMA subscription OWNER TO postgres;

--
-- Name: pgcrypto; Type: EXTENSION; Schema: -; Owner: -
--

CREATE EXTENSION IF NOT EXISTS pgcrypto WITH SCHEMA public;


--
-- Name: EXTENSION pgcrypto; Type: COMMENT; Schema: -; Owner: 
--

COMMENT ON EXTENSION pgcrypto IS 'cryptographic functions';


SET default_tablespace = '';

SET default_table_access_method = heap;

--
-- Name: session; Type: TABLE; Schema: booking; Owner: postgres
--

CREATE TABLE booking.session (
    id uuid NOT NULL,
    doctor_clinic_id uuid NOT NULL,
    shift_id uuid NOT NULL,
    session_date date NOT NULL,
    start_time time without time zone NOT NULL,
    end_time time without time zone NOT NULL,
    capacity integer NOT NULL,
    booked_count integer DEFAULT 0 NOT NULL,
    follow_up_quota integer DEFAULT 0 NOT NULL,
    follow_up_booked_count integer DEFAULT 0 NOT NULL,
    status character varying(20) DEFAULT 'ACTIVE'::character varying NOT NULL,
    created_at timestamp with time zone DEFAULT now() NOT NULL,
    updated_at timestamp with time zone DEFAULT now() NOT NULL,
    version bigint DEFAULT 0 NOT NULL,
    CONSTRAINT ck_booking_session_capacity CHECK (((capacity > 0) AND (booked_count >= 0) AND (booked_count <= capacity) AND (follow_up_quota >= 0) AND (follow_up_quota <= capacity) AND (follow_up_booked_count >= 0) AND (follow_up_booked_count <= follow_up_quota))),
    CONSTRAINT ck_booking_session_time CHECK ((start_time < end_time))
);


ALTER TABLE booking.session OWNER TO postgres;

--
-- Name: area; Type: TABLE; Schema: clinic; Owner: postgres
--

CREATE TABLE clinic.area (
    id uuid NOT NULL,
    code character varying(60) NOT NULL,
    name_en character varying(120) NOT NULL,
    name_ar character varying(120) NOT NULL,
    city character varying(100) NOT NULL,
    active boolean DEFAULT true NOT NULL,
    created_at timestamp with time zone DEFAULT now() NOT NULL,
    updated_at timestamp with time zone DEFAULT now() NOT NULL,
    version bigint DEFAULT 0 NOT NULL
);


ALTER TABLE clinic.area OWNER TO postgres;

--
-- Name: clinic; Type: TABLE; Schema: clinic; Owner: postgres
--

CREATE TABLE clinic.clinic (
    id uuid NOT NULL,
    owner_user_id uuid NOT NULL,
    area_id uuid NOT NULL,
    name character varying(150) NOT NULL,
    address character varying(300) NOT NULL,
    time_zone character varying(64) DEFAULT 'Africa/Cairo'::character varying NOT NULL,
    status character varying(32) NOT NULL,
    created_at timestamp with time zone DEFAULT now() NOT NULL,
    updated_at timestamp with time zone DEFAULT now() NOT NULL,
    version bigint DEFAULT 0 NOT NULL,
    CONSTRAINT ck_clinic_status CHECK (((status)::text = ANY ((ARRAY['PENDING_VERIFICATION'::character varying, 'ACTIVE'::character varying, 'SUSPENDED'::character varying, 'REJECTED'::character varying])::text[])))
);


ALTER TABLE clinic.clinic OWNER TO postgres;

--
-- Name: clinic_phone; Type: TABLE; Schema: clinic; Owner: postgres
--

CREATE TABLE clinic.clinic_phone (
    clinic_id uuid NOT NULL,
    phone character varying(16) NOT NULL,
    CONSTRAINT ck_clinic_phone_e164 CHECK (((phone)::text ~ '^\+[1-9][0-9]{7,14}$'::text))
);


ALTER TABLE clinic.clinic_phone OWNER TO postgres;

--
-- Name: create_idempotency; Type: TABLE; Schema: clinic; Owner: postgres
--

CREATE TABLE clinic.create_idempotency (
    actor_user_id uuid NOT NULL,
    idempotency_key character varying(100) NOT NULL,
    request_hash character(64) NOT NULL,
    clinic_id uuid,
    created_at timestamp with time zone DEFAULT now() NOT NULL
);


ALTER TABLE clinic.create_idempotency OWNER TO postgres;

--
-- Name: verification_event; Type: TABLE; Schema: clinic; Owner: postgres
--

CREATE TABLE clinic.verification_event (
    id uuid NOT NULL,
    clinic_id uuid NOT NULL,
    actor_user_id uuid NOT NULL,
    action character varying(40) NOT NULL,
    previous_status character varying(32) NOT NULL,
    new_status character varying(32) NOT NULL,
    reason character varying(1000),
    created_at timestamp with time zone DEFAULT now() NOT NULL,
    CONSTRAINT ck_clinic_verification_action CHECK (((action)::text = ANY ((ARRAY['APPROVE'::character varying, 'REJECT'::character varying, 'REQUEST_INFORMATION'::character varying, 'SUSPEND'::character varying, 'REACTIVATE'::character varying])::text[]))),
    CONSTRAINT ck_clinic_verification_new_status CHECK (((new_status)::text = ANY ((ARRAY['PENDING_VERIFICATION'::character varying, 'ACTIVE'::character varying, 'SUSPENDED'::character varying, 'REJECTED'::character varying])::text[]))),
    CONSTRAINT ck_clinic_verification_previous_status CHECK (((previous_status)::text = ANY ((ARRAY['PENDING_VERIFICATION'::character varying, 'ACTIVE'::character varying, 'SUSPENDED'::character varying, 'REJECTED'::character varying])::text[])))
);


ALTER TABLE clinic.verification_event OWNER TO postgres;

--
-- Name: doctor_profile; Type: TABLE; Schema: doctor; Owner: postgres
--

CREATE TABLE doctor.doctor_profile (
    id uuid NOT NULL,
    user_id uuid NOT NULL,
    professional_title character varying(120) NOT NULL,
    bio character varying(2000),
    years_experience integer NOT NULL,
    syndicate_number character varying(50),
    gender character varying(20) NOT NULL,
    verification_status character varying(32) NOT NULL,
    created_at timestamp with time zone DEFAULT now() NOT NULL,
    updated_at timestamp with time zone DEFAULT now() NOT NULL,
    version bigint DEFAULT 0 NOT NULL,
    CONSTRAINT ck_doctor_gender CHECK (((gender)::text = ANY ((ARRAY['MALE'::character varying, 'FEMALE'::character varying, 'UNSPECIFIED'::character varying])::text[]))),
    CONSTRAINT ck_doctor_verification_status CHECK (((verification_status)::text = ANY ((ARRAY['PENDING_VERIFICATION'::character varying, 'APPROVED'::character varying, 'NEEDS_INFORMATION'::character varying, 'REJECTED'::character varying])::text[]))),
    CONSTRAINT ck_doctor_years_experience CHECK (((years_experience >= 0) AND (years_experience <= 80)))
);


ALTER TABLE doctor.doctor_profile OWNER TO postgres;

--
-- Name: doctor_specialty; Type: TABLE; Schema: doctor; Owner: postgres
--

CREATE TABLE doctor.doctor_specialty (
    doctor_id uuid NOT NULL,
    specialty_id uuid NOT NULL
);


ALTER TABLE doctor.doctor_specialty OWNER TO postgres;

--
-- Name: specialty; Type: TABLE; Schema: doctor; Owner: postgres
--

CREATE TABLE doctor.specialty (
    id uuid NOT NULL,
    code character varying(50) NOT NULL,
    name_en character varying(120) NOT NULL,
    name_ar character varying(120) NOT NULL,
    active boolean DEFAULT true NOT NULL,
    created_at timestamp with time zone DEFAULT now() NOT NULL,
    updated_at timestamp with time zone DEFAULT now() NOT NULL,
    version bigint DEFAULT 0 NOT NULL
);


ALTER TABLE doctor.specialty OWNER TO postgres;

--
-- Name: doctor_clinic; Type: TABLE; Schema: doctor_clinic; Owner: postgres
--

CREATE TABLE doctor_clinic.doctor_clinic (
    id uuid NOT NULL,
    clinic_id uuid NOT NULL,
    doctor_id uuid NOT NULL,
    consultation_fee numeric(12,2) NOT NULL,
    follow_up_fee numeric(12,2) NOT NULL,
    follow_up_validity_days integer DEFAULT 14 NOT NULL,
    instapay_address character varying(150),
    auto_confirm boolean DEFAULT true NOT NULL,
    status character varying(20) DEFAULT 'ACTIVE'::character varying NOT NULL,
    created_at timestamp with time zone DEFAULT now() NOT NULL,
    updated_at timestamp with time zone DEFAULT now() NOT NULL,
    version bigint DEFAULT 0 NOT NULL,
    CONSTRAINT ck_doctor_clinic_consultation_fee CHECK ((consultation_fee >= (0)::numeric)),
    CONSTRAINT ck_doctor_clinic_follow_up_fee CHECK ((follow_up_fee >= (0)::numeric)),
    CONSTRAINT ck_doctor_clinic_follow_up_validity CHECK (((follow_up_validity_days >= 1) AND (follow_up_validity_days <= 365))),
    CONSTRAINT ck_doctor_clinic_status CHECK (((status)::text = ANY ((ARRAY['ACTIVE'::character varying, 'SUSPENDED'::character varying, 'ENDED'::character varying])::text[])))
);


ALTER TABLE doctor_clinic.doctor_clinic OWNER TO postgres;

--
-- Name: app_user; Type: TABLE; Schema: identity; Owner: postgres
--

CREATE TABLE identity.app_user (
    id uuid NOT NULL,
    phone character varying(16) NOT NULL,
    full_name character varying(150) NOT NULL,
    email character varying(254),
    password_hash character varying(100),
    status character varying(32) NOT NULL,
    locale character varying(8) DEFAULT 'ar'::character varying NOT NULL,
    created_at timestamp with time zone DEFAULT now() NOT NULL,
    updated_at timestamp with time zone DEFAULT now() NOT NULL,
    version bigint DEFAULT 0 NOT NULL,
    system_role character varying(16) DEFAULT 'USER'::character varying NOT NULL,
    must_change_password boolean DEFAULT false NOT NULL,
    CONSTRAINT ck_identity_user_phone_e164 CHECK (((phone)::text ~ '^\+[1-9][0-9]{7,14}$'::text)),
    CONSTRAINT ck_identity_user_status CHECK (((status)::text = ANY ((ARRAY['ACTIVE'::character varying, 'SUSPENDED'::character varying, 'PENDING_VERIFICATION'::character varying])::text[]))),
    CONSTRAINT ck_identity_user_system_role CHECK (((system_role)::text = ANY ((ARRAY['USER'::character varying, 'ADMIN'::character varying])::text[])))
);


ALTER TABLE identity.app_user OWNER TO postgres;

--
-- Name: doctor_onboarding_invite; Type: TABLE; Schema: identity; Owner: postgres
--

CREATE TABLE identity.doctor_onboarding_invite (
    id uuid NOT NULL,
    clinic_id uuid NOT NULL,
    phone character varying(16) NOT NULL,
    full_name character varying(150) NOT NULL,
    email character varying(254),
    professional_title character varying(120),
    bio character varying(2000),
    years_experience integer,
    syndicate_number character varying(50),
    gender character varying(20),
    specialty_ids text,
    token_hash character varying(64) NOT NULL,
    invited_by_user_id uuid NOT NULL,
    status character varying(32) NOT NULL,
    attempt_count integer DEFAULT 0 NOT NULL,
    expires_at timestamp with time zone NOT NULL,
    claimed_at timestamp with time zone,
    claimed_user_id uuid,
    created_at timestamp with time zone DEFAULT now() NOT NULL,
    updated_at timestamp with time zone DEFAULT now() NOT NULL,
    version bigint DEFAULT 0 NOT NULL,
    CONSTRAINT ck_identity_doc_invite_attempts CHECK (((attempt_count >= 0) AND (attempt_count <= 10))),
    CONSTRAINT ck_identity_doc_invite_gender CHECK (((gender IS NULL) OR ((gender)::text = ANY ((ARRAY['MALE'::character varying, 'FEMALE'::character varying, 'UNSPECIFIED'::character varying])::text[])))),
    CONSTRAINT ck_identity_doc_invite_status CHECK (((status)::text = ANY ((ARRAY['PENDING'::character varying, 'CLAIMED'::character varying, 'EXPIRED'::character varying, 'REVOKED'::character varying])::text[])))
);


ALTER TABLE identity.doctor_onboarding_invite OWNER TO postgres;

--
-- Name: invitation_permission; Type: TABLE; Schema: identity; Owner: postgres
--

CREATE TABLE identity.invitation_permission (
    invitation_id uuid NOT NULL,
    permission character varying(64) NOT NULL
);


ALTER TABLE identity.invitation_permission OWNER TO postgres;

--
-- Name: membership_permission; Type: TABLE; Schema: identity; Owner: postgres
--

CREATE TABLE identity.membership_permission (
    membership_id uuid NOT NULL,
    permission character varying(64) NOT NULL
);


ALTER TABLE identity.membership_permission OWNER TO postgres;

--
-- Name: otp_challenge; Type: TABLE; Schema: identity; Owner: postgres
--

CREATE TABLE identity.otp_challenge (
    id uuid NOT NULL,
    phone character varying(16) NOT NULL,
    purpose character varying(32) NOT NULL,
    code_hash character varying(100) NOT NULL,
    attempt_count integer DEFAULT 0 NOT NULL,
    expires_at timestamp with time zone NOT NULL,
    verified_token_hash character varying(64),
    verified_token_expires_at timestamp with time zone,
    consumed_at timestamp with time zone,
    created_at timestamp with time zone DEFAULT now() NOT NULL,
    updated_at timestamp with time zone DEFAULT now() NOT NULL,
    version bigint DEFAULT 0 NOT NULL,
    CONSTRAINT ck_identity_otp_attempts CHECK (((attempt_count >= 0) AND (attempt_count <= 10))),
    CONSTRAINT ck_identity_otp_purpose CHECK (((purpose)::text = ANY ((ARRAY['OWNER_REGISTRATION'::character varying, 'LOGIN'::character varying])::text[])))
);


ALTER TABLE identity.otp_challenge OWNER TO postgres;

--
-- Name: password_login_attempt; Type: TABLE; Schema: identity; Owner: postgres
--

CREATE TABLE identity.password_login_attempt (
    phone character varying(16) NOT NULL,
    failed_attempts integer DEFAULT 0 NOT NULL,
    window_started_at timestamp with time zone NOT NULL,
    locked_until timestamp with time zone,
    created_at timestamp with time zone DEFAULT now() NOT NULL,
    updated_at timestamp with time zone DEFAULT now() NOT NULL,
    version bigint DEFAULT 0 NOT NULL,
    CONSTRAINT ck_identity_password_failures CHECK (((failed_attempts >= 0) AND (failed_attempts <= 5)))
);


ALTER TABLE identity.password_login_attempt OWNER TO postgres;

--
-- Name: refresh_session; Type: TABLE; Schema: identity; Owner: postgres
--

CREATE TABLE identity.refresh_session (
    id uuid NOT NULL,
    user_id uuid NOT NULL,
    token_hash character varying(64) NOT NULL,
    expires_at timestamp with time zone NOT NULL,
    revoked_at timestamp with time zone,
    replaced_by_session_id uuid,
    device_id character varying(128),
    created_at timestamp with time zone DEFAULT now() NOT NULL,
    updated_at timestamp with time zone DEFAULT now() NOT NULL,
    version bigint DEFAULT 0 NOT NULL
);


ALTER TABLE identity.refresh_session OWNER TO postgres;

--
-- Name: staff_invitation; Type: TABLE; Schema: identity; Owner: postgres
--

CREATE TABLE identity.staff_invitation (
    id uuid NOT NULL,
    clinic_id uuid NOT NULL,
    phone character varying(16) NOT NULL,
    role character varying(32) NOT NULL,
    token_hash character varying(64) NOT NULL,
    invited_by_user_id uuid NOT NULL,
    status character varying(32) NOT NULL,
    expires_at timestamp with time zone NOT NULL,
    accepted_at timestamp with time zone,
    created_at timestamp with time zone DEFAULT now() NOT NULL,
    updated_at timestamp with time zone DEFAULT now() NOT NULL,
    version bigint DEFAULT 0 NOT NULL,
    CONSTRAINT ck_identity_invitation_role CHECK (((role)::text = ANY ((ARRAY['DOCTOR'::character varying, 'SECRETARY'::character varying, 'ACCOUNTANT'::character varying])::text[]))),
    CONSTRAINT ck_identity_invitation_status CHECK (((status)::text = ANY ((ARRAY['PENDING'::character varying, 'ACCEPTED'::character varying, 'REVOKED'::character varying, 'EXPIRED'::character varying])::text[])))
);


ALTER TABLE identity.staff_invitation OWNER TO postgres;

--
-- Name: staff_membership; Type: TABLE; Schema: identity; Owner: postgres
--

CREATE TABLE identity.staff_membership (
    id uuid NOT NULL,
    user_id uuid NOT NULL,
    clinic_id uuid NOT NULL,
    role character varying(32) NOT NULL,
    status character varying(32) NOT NULL,
    created_at timestamp with time zone DEFAULT now() NOT NULL,
    updated_at timestamp with time zone DEFAULT now() NOT NULL,
    version bigint DEFAULT 0 NOT NULL,
    CONSTRAINT ck_identity_membership_role CHECK (((role)::text = ANY ((ARRAY['OWNER'::character varying, 'DOCTOR'::character varying, 'SECRETARY'::character varying, 'ACCOUNTANT'::character varying])::text[]))),
    CONSTRAINT ck_identity_membership_status CHECK (((status)::text = ANY ((ARRAY['ACTIVE'::character varying, 'SUSPENDED'::character varying, 'REVOKED'::character varying])::text[])))
);


ALTER TABLE identity.staff_membership OWNER TO postgres;

--
-- Name: admin_action_audit; Type: TABLE; Schema: platform_admin; Owner: postgres
--

CREATE TABLE platform_admin.admin_action_audit (
    id uuid NOT NULL,
    actor_user_id uuid NOT NULL,
    action_type character varying(80) NOT NULL,
    target_type character varying(80) NOT NULL,
    target_id character varying(120),
    previous_value character varying(500),
    new_value character varying(500),
    reason character varying(1000),
    created_at timestamp with time zone DEFAULT now() NOT NULL
);


ALTER TABLE platform_admin.admin_action_audit OWNER TO postgres;

--
-- Name: admin_grant; Type: TABLE; Schema: platform_admin; Owner: postgres
--

CREATE TABLE platform_admin.admin_grant (
    id uuid NOT NULL,
    user_id uuid NOT NULL,
    status character varying(20) NOT NULL,
    granted_by uuid,
    revoked_by uuid,
    revoked_at timestamp with time zone,
    created_at timestamp with time zone DEFAULT now() NOT NULL,
    updated_at timestamp with time zone DEFAULT now() NOT NULL,
    version bigint DEFAULT 0 NOT NULL,
    CONSTRAINT ck_platform_admin_grant_status CHECK (((status)::text = ANY ((ARRAY['ACTIVE'::character varying, 'REVOKED'::character varying])::text[])))
);


ALTER TABLE platform_admin.admin_grant OWNER TO postgres;

--
-- Name: admin_grant_permission; Type: TABLE; Schema: platform_admin; Owner: postgres
--

CREATE TABLE platform_admin.admin_grant_permission (
    grant_id uuid NOT NULL,
    permission character varying(64) NOT NULL
);


ALTER TABLE platform_admin.admin_grant_permission OWNER TO postgres;

--
-- Name: event_publication; Type: TABLE; Schema: public; Owner: postgres
--

CREATE TABLE public.event_publication (
    id uuid NOT NULL,
    completion_attempts integer NOT NULL,
    completion_date timestamp with time zone,
    event_type character varying(255) NOT NULL,
    last_resubmission_date timestamp with time zone,
    listener_id character varying(255) NOT NULL,
    publication_date timestamp with time zone NOT NULL,
    serialized_event character varying(255) NOT NULL,
    status character varying(255),
    CONSTRAINT ck_event_publication_status CHECK (((status)::text = ANY ((ARRAY['PUBLISHED'::character varying, 'PROCESSING'::character varying, 'COMPLETED'::character varying, 'FAILED'::character varying, 'RESUBMITTED'::character varying])::text[])))
);


ALTER TABLE public.event_publication OWNER TO postgres;

--
-- Name: flyway_schema_history; Type: TABLE; Schema: public; Owner: postgres
--

CREATE TABLE public.flyway_schema_history (
    installed_rank integer NOT NULL,
    version character varying(50),
    description character varying(200) NOT NULL,
    type character varying(20) NOT NULL,
    script character varying(1000) NOT NULL,
    checksum integer,
    installed_by character varying(100) NOT NULL,
    installed_on timestamp without time zone DEFAULT now() NOT NULL,
    execution_time integer NOT NULL,
    success boolean NOT NULL
);


ALTER TABLE public.flyway_schema_history OWNER TO postgres;

--
-- Name: clinic_review; Type: TABLE; Schema: review; Owner: postgres
--

CREATE TABLE review.clinic_review (
    id uuid NOT NULL,
    clinic_id uuid NOT NULL,
    doctor_id uuid,
    patient_user_id uuid NOT NULL,
    rating integer NOT NULL,
    comment character varying(2000),
    status character varying(20) NOT NULL,
    moderation_reason character varying(1000),
    moderated_by uuid,
    moderated_at timestamp with time zone,
    created_at timestamp with time zone DEFAULT now() NOT NULL,
    updated_at timestamp with time zone DEFAULT now() NOT NULL,
    version bigint DEFAULT 0 NOT NULL,
    CONSTRAINT ck_review_rating CHECK (((rating >= 1) AND (rating <= 5))),
    CONSTRAINT ck_review_status CHECK (((status)::text = ANY ((ARRAY['PENDING'::character varying, 'APPROVED'::character varying, 'REJECTED'::character varying, 'HIDDEN'::character varying])::text[])))
);


ALTER TABLE review.clinic_review OWNER TO postgres;

--
-- Name: review_moderation_audit; Type: TABLE; Schema: review; Owner: postgres
--

CREATE TABLE review.review_moderation_audit (
    id uuid NOT NULL,
    review_id uuid NOT NULL,
    actor_user_id uuid NOT NULL,
    action_type character varying(40) NOT NULL,
    previous_status character varying(20),
    new_status character varying(20),
    reason character varying(1000),
    created_at timestamp with time zone DEFAULT now() NOT NULL
);


ALTER TABLE review.review_moderation_audit OWNER TO postgres;

--
-- Name: schedule_exception; Type: TABLE; Schema: scheduling; Owner: postgres
--

CREATE TABLE scheduling.schedule_exception (
    id uuid NOT NULL,
    doctor_clinic_id uuid NOT NULL,
    shift_id uuid,
    type character varying(20) NOT NULL,
    date_from date NOT NULL,
    date_to date NOT NULL,
    changed_start_time time without time zone,
    changed_end_time time without time zone,
    reason character varying(500),
    cancelled_at timestamp with time zone,
    created_at timestamp with time zone DEFAULT now() NOT NULL,
    updated_at timestamp with time zone DEFAULT now() NOT NULL,
    version bigint DEFAULT 0 NOT NULL,
    CONSTRAINT ck_scheduling_exception_dates CHECK ((date_from <= date_to)),
    CONSTRAINT ck_scheduling_exception_hours CHECK (((((type)::text = 'DAY_OFF'::text) AND (changed_start_time IS NULL) AND (changed_end_time IS NULL)) OR (((type)::text = 'CHANGED_HOURS'::text) AND (changed_start_time IS NOT NULL) AND (changed_end_time IS NOT NULL) AND (changed_start_time < changed_end_time)))),
    CONSTRAINT ck_scheduling_exception_type CHECK (((type)::text = ANY ((ARRAY['DAY_OFF'::character varying, 'CHANGED_HOURS'::character varying])::text[])))
);


ALTER TABLE scheduling.schedule_exception OWNER TO postgres;

--
-- Name: schedule_guard; Type: TABLE; Schema: scheduling; Owner: postgres
--

CREATE TABLE scheduling.schedule_guard (
    doctor_clinic_id uuid NOT NULL,
    updated_at timestamp with time zone DEFAULT now() NOT NULL
);


ALTER TABLE scheduling.schedule_guard OWNER TO postgres;

--
-- Name: shift; Type: TABLE; Schema: scheduling; Owner: postgres
--

CREATE TABLE scheduling.shift (
    id uuid NOT NULL,
    doctor_clinic_id uuid NOT NULL,
    name character varying(100) NOT NULL,
    capacity_mode character varying(20) NOT NULL,
    average_visit_minutes integer,
    fixed_capacity integer,
    effective_from date NOT NULL,
    active boolean DEFAULT true NOT NULL,
    created_at timestamp with time zone DEFAULT now() NOT NULL,
    updated_at timestamp with time zone DEFAULT now() NOT NULL,
    version bigint DEFAULT 0 NOT NULL,
    CONSTRAINT ck_scheduling_shift_capacity CHECK (((((capacity_mode)::text = 'CALCULATED'::text) AND ((average_visit_minutes >= 3) AND (average_visit_minutes <= 60)) AND (fixed_capacity IS NULL)) OR (((capacity_mode)::text = 'FIXED'::text) AND (fixed_capacity > 0) AND (average_visit_minutes IS NULL)))),
    CONSTRAINT ck_scheduling_shift_mode CHECK (((capacity_mode)::text = ANY ((ARRAY['CALCULATED'::character varying, 'FIXED'::character varying])::text[])))
);


ALTER TABLE scheduling.shift OWNER TO postgres;

--
-- Name: shift_break; Type: TABLE; Schema: scheduling; Owner: postgres
--

CREATE TABLE scheduling.shift_break (
    id uuid NOT NULL,
    rule_id uuid NOT NULL,
    start_time time without time zone NOT NULL,
    end_time time without time zone NOT NULL,
    CONSTRAINT ck_scheduling_shift_break_time CHECK ((start_time < end_time))
);


ALTER TABLE scheduling.shift_break OWNER TO postgres;

--
-- Name: weekly_rule; Type: TABLE; Schema: scheduling; Owner: postgres
--

CREATE TABLE scheduling.weekly_rule (
    id uuid NOT NULL,
    shift_id uuid NOT NULL,
    day_of_week character varying(12) NOT NULL,
    start_time time without time zone NOT NULL,
    end_time time without time zone NOT NULL,
    follow_up_quota integer DEFAULT 0 NOT NULL,
    CONSTRAINT ck_scheduling_weekly_rule_day CHECK (((day_of_week)::text = ANY ((ARRAY['MONDAY'::character varying, 'TUESDAY'::character varying, 'WEDNESDAY'::character varying, 'THURSDAY'::character varying, 'FRIDAY'::character varying, 'SATURDAY'::character varying, 'SUNDAY'::character varying])::text[]))),
    CONSTRAINT ck_scheduling_weekly_rule_quota CHECK ((follow_up_quota >= 0)),
    CONSTRAINT ck_scheduling_weekly_rule_time CHECK ((start_time < end_time))
);


ALTER TABLE scheduling.weekly_rule OWNER TO postgres;

--
-- Name: clinic_subscription; Type: TABLE; Schema: subscription; Owner: postgres
--

CREATE TABLE subscription.clinic_subscription (
    id uuid NOT NULL,
    clinic_id uuid NOT NULL,
    plan_id uuid NOT NULL,
    status character varying(20) NOT NULL,
    starts_at timestamp with time zone,
    ends_at timestamp with time zone,
    activated_by uuid,
    cancelled_by uuid,
    reason character varying(500),
    created_at timestamp with time zone DEFAULT now() NOT NULL,
    updated_at timestamp with time zone DEFAULT now() NOT NULL,
    version bigint DEFAULT 0 NOT NULL,
    CONSTRAINT ck_subscription_status CHECK (((status)::text = ANY ((ARRAY['PENDING'::character varying, 'ACTIVE'::character varying, 'EXPIRED'::character varying, 'SUSPENDED'::character varying, 'CANCELLED'::character varying])::text[])))
);


ALTER TABLE subscription.clinic_subscription OWNER TO postgres;

--
-- Name: plan; Type: TABLE; Schema: subscription; Owner: postgres
--

CREATE TABLE subscription.plan (
    id uuid NOT NULL,
    code character varying(60) NOT NULL,
    name character varying(120) NOT NULL,
    duration_days integer NOT NULL,
    price_amount numeric(12,2) NOT NULL,
    currency character varying(8) DEFAULT 'EGP'::character varying NOT NULL,
    clinic_limit integer,
    doctor_limit integer,
    active boolean DEFAULT true NOT NULL,
    created_at timestamp with time zone DEFAULT now() NOT NULL,
    updated_at timestamp with time zone DEFAULT now() NOT NULL,
    version bigint DEFAULT 0 NOT NULL,
    CONSTRAINT ck_subscription_plan_duration CHECK ((duration_days > 0)),
    CONSTRAINT ck_subscription_plan_price CHECK ((price_amount >= (0)::numeric))
);


ALTER TABLE subscription.plan OWNER TO postgres;

--
-- Data for Name: session; Type: TABLE DATA; Schema: booking; Owner: postgres
--

COPY booking.session (id, doctor_clinic_id, shift_id, session_date, start_time, end_time, capacity, booked_count, follow_up_quota, follow_up_booked_count, status, created_at, updated_at, version) FROM stdin;
f29a40b1-50c3-4782-be62-04f9b0398b08	f13f11ba-9379-4871-b77d-6fcd3b19be61	f5c6c8b7-dc86-4b0f-91a1-60d57a7c5b46	2026-11-15	09:00:00	14:00:00	19	0	2	0	ACTIVE	2026-11-11 13:59:38.273799+00	2026-11-11 13:59:38.273799+00	0
1f1b991d-5d21-49d4-b2a4-7e9bc554ea20	f13f11ba-9379-4871-b77d-6fcd3b19be61	f5c6c8b7-dc86-4b0f-91a1-60d57a7c5b46	2026-11-22	09:00:00	14:00:00	19	0	2	0	ACTIVE	2026-11-11 13:59:38.300249+00	2026-11-11 13:59:38.300249+00	0
14ccd45b-03de-4a7c-8570-632ccfb00299	f13f11ba-9379-4871-b77d-6fcd3b19be61	f5c6c8b7-dc86-4b0f-91a1-60d57a7c5b46	2026-11-29	09:00:00	14:00:00	19	0	2	0	ACTIVE	2026-11-11 13:59:38.306366+00	2026-11-11 13:59:38.306366+00	0
9565daf9-be58-4030-9501-3efda2355b8f	f13f11ba-9379-4871-b77d-6fcd3b19be61	f5c6c8b7-dc86-4b0f-91a1-60d57a7c5b46	2026-12-06	09:00:00	14:00:00	19	0	2	0	ACTIVE	2026-11-11 13:59:38.314613+00	2026-11-11 13:59:38.314613+00	0
29989276-3602-46fe-8b36-90568e711ba5	f13f11ba-9379-4871-b77d-6fcd3b19be61	f5c6c8b7-dc86-4b0f-91a1-60d57a7c5b46	2026-12-13	09:00:00	14:00:00	19	0	2	0	ACTIVE	2026-11-11 13:59:38.324288+00	2026-11-11 13:59:38.324288+00	0
b183dfa5-2415-40f0-ba01-77806128ffaa	f13f11ba-9379-4871-b77d-6fcd3b19be61	f5c6c8b7-dc86-4b0f-91a1-60d57a7c5b46	2026-12-20	09:00:00	14:00:00	19	0	2	0	ACTIVE	2026-11-11 13:59:38.334728+00	2026-11-11 13:59:38.334728+00	0
c071b77a-7c86-4af6-b282-a82d12e180a4	f13f11ba-9379-4871-b77d-6fcd3b19be61	f5c6c8b7-dc86-4b0f-91a1-60d57a7c5b46	2026-12-27	09:00:00	14:00:00	19	0	2	0	ACTIVE	2026-11-11 13:59:38.345682+00	2026-11-11 13:59:38.345682+00	0
69dec5f6-1a02-4329-baf8-68d4e46bafbb	f13f11ba-9379-4871-b77d-6fcd3b19be61	f5c6c8b7-dc86-4b0f-91a1-60d57a7c5b46	2027-01-03	09:00:00	14:00:00	19	0	2	0	ACTIVE	2026-11-11 13:59:38.354356+00	2026-11-11 13:59:38.354356+00	0
\.


--
-- Data for Name: area; Type: TABLE DATA; Schema: clinic; Owner: postgres
--

COPY clinic.area (id, code, name_en, name_ar, city, active, created_at, updated_at, version) FROM stdin;
20000000-0000-0000-0000-000000000001	DOKKI	Dokki	الدقي	Giza	t	2026-11-04 13:20:56.357415+00	2026-11-04 13:20:56.357415+00	0
20000000-0000-0000-0000-000000000002	MOHANDESSEEN	Mohandesseen	المهندسين	Giza	t	2026-11-04 13:20:56.357415+00	2026-11-04 13:20:56.357415+00	0
20000000-0000-0000-0000-000000000003	HARAM	Haram	الهرم	Giza	t	2026-11-04 13:20:56.357415+00	2026-11-04 13:20:56.357415+00	0
20000000-0000-0000-0000-000000000004	SHEIKH_ZAYED	Sheikh Zayed	الشيخ زايد	Giza	t	2026-11-04 13:20:56.357415+00	2026-11-04 13:20:56.357415+00	0
20000000-0000-0000-0000-000000000005	NASR_CITY	Nasr City	مدينة نصر	Cairo	t	2026-11-04 13:20:56.357415+00	2026-11-04 13:20:56.357415+00	0
20000000-0000-0000-0000-000000000006	MAADI	Maadi	المعادي	Cairo	t	2026-11-04 13:20:56.357415+00	2026-11-04 13:20:56.357415+00	0
20000000-0000-0000-0000-000000000007	HELIOPOLIS	Heliopolis	مصر الجديدة	Cairo	t	2026-11-04 13:20:56.357415+00	2026-11-04 13:20:56.357415+00	0
20000000-0000-0000-0000-000000000008	NEW_CAPITAL	New Capital	العاصمة الإدارية	New Administrative Capital	t	2026-11-04 13:20:56.357415+00	2026-11-04 13:20:56.357415+00	0
20000000-0000-0000-0000-000000000009	SMOUHA	Smouha	سموحة	Alexandria	t	2026-11-04 13:20:56.357415+00	2026-11-04 13:20:56.357415+00	0
20000000-0000-0000-0000-000000000010	MIAMI_ALEX	Miami	ميامي	Alexandria	t	2026-11-04 13:20:56.357415+00	2026-11-04 13:20:56.357415+00	0
20000000-0000-0000-0000-000000000011	STANLEY	Stanley	ستانلي	Alexandria	t	2026-11-04 13:20:56.357415+00	2026-11-04 13:20:56.357415+00	0
20000000-0000-0000-0000-000000000012	MANDARA	Mandara	المندرة	Alexandria	t	2026-11-04 13:20:56.357415+00	2026-11-04 13:20:56.357415+00	0
\.


--
-- Data for Name: clinic; Type: TABLE DATA; Schema: clinic; Owner: postgres
--

COPY clinic.clinic (id, owner_user_id, area_id, name, address, time_zone, status, created_at, updated_at, version) FROM stdin;
9039cc7c-ac66-4adc-a443-f5cd3f213b57	9159027d-2903-43a9-84b2-726b8196c34c	20000000-0000-0000-0000-000000000001	Kom Hamada Clinic	12 Test Street	Africa/Cairo	PENDING_VERIFICATION	2026-11-04 15:38:52.575401+00	2026-11-04 15:38:54.036735+00	1
4e02b20e-0311-49c8-90ca-b1069382ff6e	9159027d-2903-43a9-84b2-726b8196c34c	20000000-0000-0000-0000-000000000001	Haram special Clinic	12 Test Street	Africa/Cairo	PENDING_VERIFICATION	2026-11-06 17:10:22.412334+00	2026-11-06 17:10:22.783228+00	1
fb420eb5-dcc2-4eb0-af42-7a1ffdcf7b3f	bc1b2b1c-c1f9-4ee9-8d8b-cceba10dda16	20000000-0000-0000-0000-000000000001	new haram special Clinic	12 Test Street	Africa/Cairo	PENDING_VERIFICATION	2026-11-06 17:16:24.800339+00	2026-11-06 17:16:24.80611+00	1
5e5fdfca-7e79-4d6d-838d-1b938d07a9e4	6496523f-63c7-4a39-b8db-8ae492981b19	20000000-0000-0000-0000-000000000001	BankMasr Clinic	12 Test Street	Africa/Cairo	PENDING_VERIFICATION	2026-11-11 13:28:39.284259+00	2026-11-11 13:28:39.33292+00	1
\.


--
-- Data for Name: clinic_phone; Type: TABLE DATA; Schema: clinic; Owner: postgres
--

COPY clinic.clinic_phone (clinic_id, phone) FROM stdin;
9039cc7c-ac66-4adc-a443-f5cd3f213b57	+201012345678
4e02b20e-0311-49c8-90ca-b1069382ff6e	+201012345678
fb420eb5-dcc2-4eb0-af42-7a1ffdcf7b3f	+201012345678
5e5fdfca-7e79-4d6d-838d-1b938d07a9e4	+201012345678
\.


--
-- Data for Name: create_idempotency; Type: TABLE DATA; Schema: clinic; Owner: postgres
--

COPY clinic.create_idempotency (actor_user_id, idempotency_key, request_hash, clinic_id, created_at) FROM stdin;
9159027d-2903-43a9-84b2-726b8196c34c	clinic-1793806732007	b429ac19285c5b31e9590764e393d66d21c227889ef52026f9623964ce078298	9039cc7c-ac66-4adc-a443-f5cd3f213b57	2026-11-04 15:38:52.272053+00
9159027d-2903-43a9-84b2-726b8196c34c	clinic-1793985021875	b007330d33ef080ab44e3599ba74245a03699a8bf196e84618cf3af4ef9610c1	4e02b20e-0311-49c8-90ca-b1069382ff6e	2026-11-06 17:10:22.180331+00
bc1b2b1c-c1f9-4ee9-8d8b-cceba10dda16	clinic-1793985384445	482bea5d1938ea30333a4131645298d602b84387cf2cd629223b71676e59c018	fb420eb5-dcc2-4eb0-af42-7a1ffdcf7b3f	2026-11-06 17:16:24.694791+00
6496523f-63c7-4a39-b8db-8ae492981b19	clinic-1794403718464	8524708b640064c2a49748eb006ae6ba04275410d4a560f85b3a697548a9ac83	5e5fdfca-7e79-4d6d-838d-1b938d07a9e4	2026-11-11 13:28:38.890527+00
\.


--
-- Data for Name: verification_event; Type: TABLE DATA; Schema: clinic; Owner: postgres
--

COPY clinic.verification_event (id, clinic_id, actor_user_id, action, previous_status, new_status, reason, created_at) FROM stdin;
\.


--
-- Data for Name: doctor_profile; Type: TABLE DATA; Schema: doctor; Owner: postgres
--

COPY doctor.doctor_profile (id, user_id, professional_title, bio, years_experience, syndicate_number, gender, verification_status, created_at, updated_at, version) FROM stdin;
76a7d68b-a7f2-49c1-b252-5ff6229f6dab	5cc50af5-a72e-4bce-b23f-392ec44767b1	string	string	80	string	MALE	PENDING_VERIFICATION	2026-11-06 11:12:37.532617+00	2026-11-06 11:12:37.551971+00	1
74f6da81-438c-4bf4-a4d1-04adc77d269d	b1230650-5dd9-4500-bbf0-ffb4eec55888	string	string	80	12355	MALE	PENDING_VERIFICATION	2026-11-06 11:21:01.847146+00	2026-11-06 11:21:01.853457+00	1
cd1e1952-5fea-41fc-bc99-82a104f88732	d8aedf8b-1783-4b08-8c12-c537f5a53524	string	string	80	string1	MALE	PENDING_VERIFICATION	2026-11-06 12:29:31.551779+00	2026-11-06 12:29:31.558049+00	1
96a8f21c-a017-4638-b2ad-b18a956430bb	c18e5dff-ba0d-4d81-8b36-508304b222da	string	string	15	248521	MALE	PENDING_VERIFICATION	2026-11-06 15:48:57.887602+00	2026-11-06 15:48:57.890358+00	1
c0c0392f-3d6a-4d91-ba48-df17327d5c57	038b411f-27c5-42af-b297-32ef1e5ec929	Consultant	Provisioned directly	5	SYN-001	UNSPECIFIED	PENDING_VERIFICATION	2026-11-06 17:05:44.922886+00	2026-11-06 17:05:45.278933+00	1
f3c0acc5-6813-4a15-a704-7293fc074eed	5e5b2859-2a87-4d44-a05a-c0c3777c4125	Consultant	Provisioned directly	5	SYN-002	UNSPECIFIED	PENDING_VERIFICATION	2026-11-06 19:30:19.364889+00	2026-11-06 19:30:19.370109+00	1
55df2479-396b-4171-b44c-751d7766f462	cedc8f2f-f2ba-42fe-a03b-de4ff1ff2f56	Consultant	Completed after claim	4	SYN-CLAIM-001	UNSPECIFIED	PENDING_VERIFICATION	2026-11-11 13:53:19.612668+00	2026-11-11 13:53:20.000549+00	1
\.


--
-- Data for Name: doctor_specialty; Type: TABLE DATA; Schema: doctor; Owner: postgres
--

COPY doctor.doctor_specialty (doctor_id, specialty_id) FROM stdin;
76a7d68b-a7f2-49c1-b252-5ff6229f6dab	10000000-0000-0000-0000-000000000001
74f6da81-438c-4bf4-a4d1-04adc77d269d	10000000-0000-0000-0000-000000000001
cd1e1952-5fea-41fc-bc99-82a104f88732	10000000-0000-0000-0000-000000000001
96a8f21c-a017-4638-b2ad-b18a956430bb	10000000-0000-0000-0000-000000000012
c0c0392f-3d6a-4d91-ba48-df17327d5c57	10000000-0000-0000-0000-000000000003
f3c0acc5-6813-4a15-a704-7293fc074eed	10000000-0000-0000-0000-000000000003
55df2479-396b-4171-b44c-751d7766f462	10000000-0000-0000-0000-000000000003
\.


--
-- Data for Name: specialty; Type: TABLE DATA; Schema: doctor; Owner: postgres
--

COPY doctor.specialty (id, code, name_en, name_ar, active, created_at, updated_at, version) FROM stdin;
10000000-0000-0000-0000-000000000001	DERMATOLOGY	Dermatology	جلدية	t	2026-11-04 13:20:56.357415+00	2026-11-04 13:20:56.357415+00	0
10000000-0000-0000-0000-000000000002	PEDIATRICS	Pediatrics	أطفال	t	2026-11-04 13:20:56.357415+00	2026-11-04 13:20:56.357415+00	0
10000000-0000-0000-0000-000000000003	CARDIOLOGY	Cardiology	قلب	t	2026-11-04 13:20:56.357415+00	2026-11-04 13:20:56.357415+00	0
10000000-0000-0000-0000-000000000004	GYNECOLOGY	Gynecology	نساء وتوليد	t	2026-11-04 13:20:56.357415+00	2026-11-04 13:20:56.357415+00	0
10000000-0000-0000-0000-000000000005	ORTHOPEDICS	Orthopedics	عظام	t	2026-11-04 13:20:56.357415+00	2026-11-04 13:20:56.357415+00	0
10000000-0000-0000-0000-000000000006	OPHTHALMOLOGY	Ophthalmology	عيون	t	2026-11-04 13:20:56.357415+00	2026-11-04 13:20:56.357415+00	0
10000000-0000-0000-0000-000000000007	DENTISTRY	Dentistry	أسنان	t	2026-11-04 13:20:56.357415+00	2026-11-04 13:20:56.357415+00	0
10000000-0000-0000-0000-000000000008	INTERNAL_MEDICINE	Internal Medicine	باطنة	t	2026-11-04 13:20:56.357415+00	2026-11-04 13:20:56.357415+00	0
10000000-0000-0000-0000-000000000009	NEUROLOGY	Neurology	مخ وأعصاب	t	2026-11-04 13:20:56.357415+00	2026-11-04 13:20:56.357415+00	0
10000000-0000-0000-0000-000000000010	PSYCHIATRY	Psychiatry	طب نفسي	t	2026-11-04 13:20:56.357415+00	2026-11-04 13:20:56.357415+00	0
10000000-0000-0000-0000-000000000011	UROLOGY	Urology	مسالك بولية	t	2026-11-04 13:20:56.357415+00	2026-11-04 13:20:56.357415+00	0
10000000-0000-0000-0000-000000000012	GENERAL_SURGERY	General Surgery	جراحة عامة	t	2026-11-04 13:20:56.357415+00	2026-11-04 13:20:56.357415+00	0
\.


--
-- Data for Name: doctor_clinic; Type: TABLE DATA; Schema: doctor_clinic; Owner: postgres
--

COPY doctor_clinic.doctor_clinic (id, clinic_id, doctor_id, consultation_fee, follow_up_fee, follow_up_validity_days, instapay_address, auto_confirm, status, created_at, updated_at, version) FROM stdin;
381699f4-32e6-4a3c-ba24-dc0a1fe5bff3	fb420eb5-dcc2-4eb0-af42-7a1ffdcf7b3f	f3c0acc5-6813-4a15-a704-7293fc074eed	150.00	75.00	14	doctor@instapay	t	ACTIVE	2026-11-06 19:32:59.472859+00	2026-11-06 19:32:59.472859+00	0
f13f11ba-9379-4871-b77d-6fcd3b19be61	5e5fdfca-7e79-4d6d-838d-1b938d07a9e4	55df2479-396b-4171-b44c-751d7766f462	150.00	75.00	14	doctor@instapay	t	ACTIVE	2026-11-11 13:56:08.883114+00	2026-11-11 13:56:08.883114+00	0
\.


--
-- Data for Name: app_user; Type: TABLE DATA; Schema: identity; Owner: postgres
--

COPY identity.app_user (id, phone, full_name, email, password_hash, status, locale, created_at, updated_at, version, system_role, must_change_password) FROM stdin;
9159027d-2903-43a9-84b2-726b8196c34c	+201032836581	Test Owner	owner@example.com	$2a$12$nQCWHDRmXsItj7b/owx1SeYPxD8LPjZMuwqaMGFU/Nz9IweXL5VfG	ACTIVE	ar	2026-11-04 15:33:35.41254+00	2026-11-04 15:33:35.41254+00	0	USER	f
e913de7f-49b5-4ae9-9627-3cf55d9b5982	+201020061563	Test Doctor	\N	\N	ACTIVE	en	2026-11-05 15:34:30.880115+00	2026-11-05 15:34:30.880115+00	0	USER	f
9f8a3d65-8c96-4667-8064-87b077f1f505	+201000000001	System Admin	admin@clinic.local	$2a$12$8oyEXG7s83F.mCn9ykcC5.N8bj3QaYJ9R.HTXLX1kIIJGpfcdScjO	ACTIVE	ar	2026-11-05 23:58:27.725273+00	2026-11-06 10:54:29.201906+00	0	ADMIN	f
5cc50af5-a72e-4bce-b23f-392ec44767b1	+9803698142189	string	user@example.com	$2a$12$xYA8vGGYtBKvDWhGyGYiRu0QH40x4QtFZEzbhB.aetDG9YSscIJAW	ACTIVE	ar	2026-11-06 11:12:37.210298+00	2026-11-06 11:12:37.210298+00	0	USER	f
b1230650-5dd9-4500-bbf0-ffb4eec55888	+9903698142186	ziads	user@examplee.com	$2a$12$BForxIpxfHYoCUgvuDeT7Orfd68f/AtHQ8qlgyyRIUw0ZLXV9LFCO	ACTIVE	ar	2026-11-06 11:21:01.693485+00	2026-11-06 11:21:01.693485+00	0	USER	f
d8aedf8b-1783-4b08-8c12-c537f5a53524	+664767225026	zoz	user@examplew.com	$2a$12$Lu29ygygMjdx68wBlI7Sb.v8D7Ll9wGuHnJjZtNTx2Ah5/4/wWt7.	ACTIVE	ar	2026-11-06 12:29:31.356997+00	2026-11-06 12:29:31.356997+00	0	USER	f
c18e5dff-ba0d-4d81-8b36-508304b222da	+8586472001333	ali	user@eeexample.com	$2a$12$Haj1YsbEdNtHiPztOGAOzOyeNPWCEzpkJLv2bU/Nzt8PUp6MeA51i	ACTIVE	ar	2026-11-06 15:48:57.821573+00	2026-11-06 15:48:57.821573+00	0	USER	t
9177c878-70ff-4a0b-8668-1c2ec055bd0e	+201020061566	Provisioned Owner	owner@clinic.local	$2a$12$1OIw8gxiay8j1jLOAaaBLuYpkPCqZzwlsxJRyxF51TiXWNEHDGkWS	ACTIVE	ar	2026-11-06 17:00:56.754284+00	2026-11-06 17:00:56.754284+00	0	USER	f
bc1b2b1c-c1f9-4ee9-8d8b-cceba10dda16	+201020061567	Provisioned Owner	owner@clinic.locall	$2a$12$3ueeHBu.88ZiaHai3rXFEOAr46xhwNlcJGTp/sWXtYTYJxLE6p9la	ACTIVE	ar	2026-11-06 17:01:32.075936+00	2026-11-06 17:01:32.075936+00	0	USER	f
038b411f-27c5-42af-b297-32ef1e5ec929	+201020061569	Provisioned Doctor	doctor@clinic.local	$2a$12$kT6OFeb1QEpFeeeR0fjjWeBQ3C0WgODWTtQeqoBOCNxKfqMAeuhYK	ACTIVE	ar	2026-11-06 17:05:44.699551+00	2026-11-06 17:05:44.699551+00	0	USER	t
5e5b2859-2a87-4d44-a05a-c0c3777c4125	+201020061522	ziadsalah9	ziads5933@gmail.com	$2a$12$5sA/Z.Dl5yNEN57V..Y03uQHR4YinPRKtH0kJCZ2ZfTmUhh09lFze	ACTIVE	ar	2026-11-06 19:30:19.231753+00	2026-11-06 19:32:03.118674+00	1	USER	f
26d1c678-9e5d-441c-b2e2-fd9147fa33fc	+201020061568	ziad salah	admin@gymhub.com	$2a$12$4DPmxhY6DFhZ93qWu89na.S0dtBfXbZ3bLL5KCr.7Inku/WiOxLKG	ACTIVE	ar	2026-11-09 14:16:12.975456+00	2026-11-09 14:16:12.975456+00	0	USER	f
eda23be8-4d83-4178-a5d9-294f5cb90a6f	+664767225022	zizo	abofatma394@gmail.com	$2a$12$CQTtx0AZoCbcmxFAFtNH1uy3l1whqrqEDxxpfLDuPC.jdxuh9OT4u	ACTIVE	ar	2026-11-10 22:53:24.299747+00	2026-11-10 22:53:24.299747+00	0	USER	f
6496523f-63c7-4a39-b8db-8ae492981b19	+201098448445	Ayat	ayat@gmail.com	$2a$12$zB3C3z5EGNGrC.YVmFyoaOZOWtYyDNY5zqftpxbl/7/W3/9Gl4qSS	ACTIVE	ar	2026-11-11 13:25:39.687429+00	2026-11-11 13:25:39.687429+00	0	USER	f
cedc8f2f-f2ba-42fe-a03b-de4ff1ff2f56	+201098448449	Ayatt	recruitmentv009@gmail.com	$2a$12$Hf750i.Uq0wWvuJo4fqi5eBCJHnIBjD7mskSEOwwLwtcpYeO02JSu	ACTIVE	ar	2026-11-11 13:51:38.699127+00	2026-11-11 13:51:38.699127+00	0	USER	f
\.


--
-- Data for Name: doctor_onboarding_invite; Type: TABLE DATA; Schema: identity; Owner: postgres
--

COPY identity.doctor_onboarding_invite (id, clinic_id, phone, full_name, email, professional_title, bio, years_experience, syndicate_number, gender, specialty_ids, token_hash, invited_by_user_id, status, attempt_count, expires_at, claimed_at, claimed_user_id, created_at, updated_at, version) FROM stdin;
8345c54c-605d-4d14-b2dc-4b081e7afd25	fb420eb5-dcc2-4eb0-af42-7a1ffdcf7b3f	+664767225022	zizo	abofatma394@gmail.com	\N	\N	\N	\N	\N	\N	25e6e1954cffe5fea16eef4763b6fd1e665ebb5a76c96dc6d4e5af27f1c69811	bc1b2b1c-c1f9-4ee9-8d8b-cceba10dda16	PENDING	0	2026-11-09 19:35:58.687703+00	\N	\N	2026-11-06 19:35:58.784203+00	2026-11-06 19:35:58.784203+00	0
4d4a7d6d-c350-4bd0-be80-a09c9f32dd81	fb420eb5-dcc2-4eb0-af42-7a1ffdcf7b3f	+664767225022	zizo	abofatma394@gmail.com	\N	\N	\N	\N	\N	\N	59e9e90246dde2d6c3350eca005fdad51a342570c9af888972be70a3dc0c3252	bc1b2b1c-c1f9-4ee9-8d8b-cceba10dda16	PENDING	0	2026-11-09 20:17:40.140959+00	\N	\N	2026-11-06 20:17:40.240662+00	2026-11-06 20:17:40.240662+00	0
6ecba04d-5b4c-4b11-a7da-4bf04d7ca60f	fb420eb5-dcc2-4eb0-af42-7a1ffdcf7b3f	+664767225022	zizo	abofatma394@gmail.com	\N	\N	\N	\N	\N	\N	99391c76ce3c811c8b603e66921bc114e06e38dbccaf26c611ddc3d4bd2ac4f6	bc1b2b1c-c1f9-4ee9-8d8b-cceba10dda16	PENDING	0	2026-11-09 20:21:37.127179+00	\N	\N	2026-11-06 20:21:37.349686+00	2026-11-06 20:21:37.349686+00	0
bba24a62-ede1-44db-b3e8-66665274b5d4	fb420eb5-dcc2-4eb0-af42-7a1ffdcf7b3f	+664767225022	zizo	abofatma394@gmail.com	\N	\N	\N	\N	\N	\N	494da4e0f2ec95bb6404b56eff1ffd2562ac402cb8ec6d8b62c70e0dc73262e0	bc1b2b1c-c1f9-4ee9-8d8b-cceba10dda16	PENDING	0	2026-11-09 20:21:46.646907+00	\N	\N	2026-11-06 20:21:46.696456+00	2026-11-06 20:21:46.696456+00	0
e9a1fce8-f149-4272-a8d7-ff233e297a35	fb420eb5-dcc2-4eb0-af42-7a1ffdcf7b3f	+201020061521	ziad salah	abofatma394@gmail.com	\N	\N	\N	\N	\N	\N	27cfe8f092c762cecb3c45eef00f96a47f3f79e9320cd8c79ce74f8fda7aa8dd	9f8a3d65-8c96-4667-8064-87b077f1f505	PENDING	0	2026-11-12 14:17:36.582921+00	\N	\N	2026-11-09 14:17:36.605816+00	2026-11-09 14:17:36.605816+00	0
3a461e89-6efc-43b2-9f7c-6c67610a8186	fb420eb5-dcc2-4eb0-af42-7a1ffdcf7b3f	+664767225022	zizo	abofatma394@gmail.com	\N	\N	\N	\N	\N	\N	27b91c18632886a4d31211e55dcad6be16f9ad1b4d014f1f311497899dbaf9d4	bc1b2b1c-c1f9-4ee9-8d8b-cceba10dda16	CLAIMED	0	2026-11-13 22:40:11.285053+00	2026-11-10 22:53:22.810435+00	eda23be8-4d83-4178-a5d9-294f5cb90a6f	2026-11-10 22:40:11.389194+00	2026-11-10 22:53:25.534449+00	1
9ffd5b9f-e037-45bb-9b04-2d72cf8734a2	5e5fdfca-7e79-4d6d-838d-1b938d07a9e4	+201098448443	Ayat	ziads5933@gmail.com	\N	\N	\N	\N	\N	\N	76b75c0f5a82188a6a3b7c1259dc76760b96740f5e8b21b80ba6fbe759f8f990	6496523f-63c7-4a39-b8db-8ae492981b19	PENDING	0	2026-11-14 13:36:05.267517+00	\N	\N	2026-11-11 13:36:05.322884+00	2026-11-11 13:36:05.322884+00	0
386a409d-96f8-4de0-99ed-1480770b4312	5e5fdfca-7e79-4d6d-838d-1b938d07a9e4	+201098448443	Ayat	ziads5933@gmail.com	\N	\N	\N	\N	\N	\N	06c1c4629f44a9569a4f95864a3003ead6880e0ad1bb0cab2fddd3c92091c860	6496523f-63c7-4a39-b8db-8ae492981b19	PENDING	0	2026-11-14 13:40:08.740278+00	\N	\N	2026-11-11 13:40:09.149385+00	2026-11-11 13:40:09.149385+00	0
2528074f-fc9e-4697-96f9-25ebe0a1cc4a	5e5fdfca-7e79-4d6d-838d-1b938d07a9e4	+201098448442	Ayat	abofatma394@gmail.com	\N	\N	\N	\N	\N	\N	6eb47729b8849401807fa170ed8b88a21479bf4e4a5bde088c4ee48da2c1764c	6496523f-63c7-4a39-b8db-8ae492981b19	PENDING	0	2026-11-14 13:47:00.941721+00	\N	\N	2026-11-11 13:47:01.005411+00	2026-11-11 13:47:01.005411+00	0
6594e490-b386-4219-9938-b19f880d5d4b	5e5fdfca-7e79-4d6d-838d-1b938d07a9e4	+201098448449	Ayatt	recruitmentv009@gmail.com	\N	\N	\N	\N	\N	\N	f4bcf470dc2e15eacde0323c2466291793f94751ec5ee6642c223dfc6b15f0b7	6496523f-63c7-4a39-b8db-8ae492981b19	CLAIMED	0	2026-11-14 13:50:39.414796+00	2026-11-11 13:51:38.183215+00	cedc8f2f-f2ba-42fe-a03b-de4ff1ff2f56	2026-11-11 13:50:39.491509+00	2026-11-11 13:51:38.975931+00	1
\.


--
-- Data for Name: invitation_permission; Type: TABLE DATA; Schema: identity; Owner: postgres
--

COPY identity.invitation_permission (invitation_id, permission) FROM stdin;
e213694d-3691-4970-9f11-81129490afdc	SCHEDULE_MANAGE
e213694d-3691-4970-9f11-81129490afdc	PRICES_MANAGE
46add768-58d3-41cf-b2f9-9d7e4890b73f	SCHEDULE_MANAGE
46add768-58d3-41cf-b2f9-9d7e4890b73f	PRICES_MANAGE
\.


--
-- Data for Name: membership_permission; Type: TABLE DATA; Schema: identity; Owner: postgres
--

COPY identity.membership_permission (membership_id, permission) FROM stdin;
9ad29e57-b525-4b60-ba4c-47e0e57d9a69	SCHEDULE_MANAGE
9ad29e57-b525-4b60-ba4c-47e0e57d9a69	PRICES_MANAGE
\.


--
-- Data for Name: otp_challenge; Type: TABLE DATA; Schema: identity; Owner: postgres
--

COPY identity.otp_challenge (id, phone, purpose, code_hash, attempt_count, expires_at, verified_token_hash, verified_token_expires_at, consumed_at, created_at, updated_at, version) FROM stdin;
984f0889-d5ed-454b-869b-9099df690db7	+201032836581	OWNER_REGISTRATION	$2a$12$Hf1QCZFXLS6AU2yIUYd.XOYy9u66pi1bm0lcNmZRYc1tGjlmrnByG	1	2026-11-04 14:57:53.404158+00	\N	\N	\N	2026-11-04 14:52:55.06849+00	2026-11-04 14:53:46.094771+00	1
dddce5c7-4210-4922-8054-b0b0bd63ae16	+201032836581	OWNER_REGISTRATION	$2a$12$TMjK9HsXvaNBfyb00rk6kulA/k.embnNF9V3hyzR4D/Uv8mDf.2Be	0	2026-11-04 15:34:40.088403+00	4c9d11be372cb8375c9d9e6b22e6878d0cff5f46d41262eb17be8b444fb0c7c6	2026-11-04 15:38:19.833045+00	2026-11-04 15:33:35.120958+00	2026-11-04 15:29:41.307335+00	2026-11-04 15:33:35.439933+00	2
c929f30e-7595-430e-9646-53c19e07a60c	+201020061562	LOGIN	$2a$12$OL3RbrMJ/qUG3.HjDi80r.KWq.gBD/dvuABFI.tYlblF/IvjXpT.y	0	2026-11-04 15:48:00.951835+00	\N	\N	\N	2026-11-04 15:43:01.262968+00	2026-11-04 15:43:01.262968+00	0
d3d0f29d-9f1a-4188-b8a8-85cba2107928	+201032836581	OWNER_REGISTRATION	$2a$12$OFREWAXo.LEpdVvtbh.LCuynHgx8wHnCcX6LYD5iXy00vY4xE5JFG	1	2026-11-05 14:22:42.003933+00	\N	\N	\N	2026-11-05 14:17:43.204978+00	2026-11-05 14:18:07.450654+00	1
36af415f-a3dc-42d1-ade3-03f7d0ee3322	+201032836581	OWNER_REGISTRATION	$2a$12$e9CdIm0M6mNCnWSIoGbfZuq4ejGixZ/2Lgbh.ytqfMoDomuffswcC	0	2026-11-05 14:32:45.767896+00	\N	\N	\N	2026-11-05 14:27:46.520452+00	2026-11-05 14:27:46.520452+00	0
988f27c0-6292-49be-b927-b8745aa68191	+201032836581	OWNER_REGISTRATION	$2a$12$U/LzMFc8IfIJmOLMegKR0.cejwqVPz7T.qZ4a7g2EeYuzSoq4gajK	0	2026-11-05 14:34:53.13479+00	\N	\N	\N	2026-11-05 14:29:53.748261+00	2026-11-05 14:29:53.748261+00	0
fcd01615-0aa1-40b4-b89b-d000c711c5ee	+201032836582	OWNER_REGISTRATION	$2a$12$hQOT5se9RtNcSCoSZzLdK.Opf/l6rNj/nblCl9ZHr5/NKB/vvJCR6	0	2026-11-05 14:35:35.621877+00	89d6a74c4a1370714cf4581b6b75827a9f44c94b1890abe28bc0cf092b38d7da	2026-11-05 14:38:45.210942+00	\N	2026-11-05 14:30:35.988064+00	2026-11-05 14:33:45.550391+00	1
4b5807b9-d68e-4a02-ab98-4277bcb897ea	+201020061562	LOGIN	$2a$12$CRHNo.RcRZYEmvOrn2p15ebXd52AAfqIHf6aQ1xaJebUP6vIiLLa6	0	2026-11-05 15:57:47.596989+00	\N	\N	\N	2026-11-05 15:52:48.540987+00	2026-11-05 15:52:48.540987+00	0
c65d2752-d12c-42bd-9a1d-6437fa39ba0e	+201020061562	LOGIN	$2a$12$1VK9xz.ocpimjsWBJGiecOTd.7QZ2bnrNOaU/y7mtAMAPEj0PICxa	0	2026-11-05 15:58:03.135031+00	\N	\N	\N	2026-11-05 15:53:03.551444+00	2026-11-05 15:53:03.551444+00	0
\.


--
-- Data for Name: password_login_attempt; Type: TABLE DATA; Schema: identity; Owner: postgres
--

COPY identity.password_login_attempt (phone, failed_attempts, window_started_at, locked_until, created_at, updated_at, version) FROM stdin;
+7075049984055	1	2026-11-06 11:11:03.484101+00	\N	2026-11-06 11:11:03.54995+00	2026-11-06 11:11:03.54995+00	0
+201020061562	2	2026-11-06 16:54:39.551923+00	\N	2026-11-06 16:54:40.899316+00	2026-11-06 16:54:53.159824+00	1
+201032839581	2	2026-11-06 16:55:08.217819+00	\N	2026-11-06 16:55:08.238499+00	2026-11-06 16:55:15.397553+00	1
+201020061569	1	2026-11-06 19:27:39.416372+00	\N	2026-11-06 17:35:50.218753+00	2026-11-06 19:27:41.463815+00	4
+201098448444	1	2026-11-11 13:43:22.459316+00	\N	2026-11-11 13:43:22.485738+00	2026-11-11 13:43:22.485738+00	0
+201098448443	2	2026-11-11 13:45:47.782514+00	\N	2026-11-11 13:45:47.824437+00	2026-11-11 13:45:55.648436+00	1
\.


--
-- Data for Name: refresh_session; Type: TABLE DATA; Schema: identity; Owner: postgres
--

COPY identity.refresh_session (id, user_id, token_hash, expires_at, revoked_at, replaced_by_session_id, device_id, created_at, updated_at, version) FROM stdin;
ebddfbb9-c14a-4481-9f0d-3f97311746f2	9159027d-2903-43a9-84b2-726b8196c34c	4970aaa705be6ac2966c992357a6b353e18ca58b2cd8281e321420f6965cfa48	2026-12-04 15:36:56.24036+00	\N	\N	device-123	2026-11-04 15:36:56.298077+00	2026-11-04 15:36:56.298077+00	0
f6515c4a-171f-4cc9-9fec-f84ee65944fe	9159027d-2903-43a9-84b2-726b8196c34c	a45db8a9ac4c9f381462e81e3a870ce94f614528014f5588924126ad093d0e7a	2026-12-04 15:37:07.164336+00	\N	\N	device-1233	2026-11-04 15:37:07.187397+00	2026-11-04 15:37:07.187397+00	0
348fc99d-e1a4-4a9d-a84d-7c02595c0ca2	9159027d-2903-43a9-84b2-726b8196c34c	c688ab050dea50bd44662bab563991fe8965c23f98d08efcbc7a735b1d2ce8e1	2026-12-05 14:34:26.358704+00	\N	\N	device-1233	2026-11-05 14:34:26.380762+00	2026-11-05 14:34:26.380762+00	0
f2bf512a-d8da-46ed-9fff-979ccafb5fd7	9159027d-2903-43a9-84b2-726b8196c34c	05cab1d2814a106340598aaeaa4789a9e0ec186accd4f04f68b0737567bbf406	2026-12-05 15:00:40.387988+00	\N	\N	device-1233	2026-11-05 15:00:40.472192+00	2026-11-05 15:00:40.472192+00	0
6924efca-a670-4909-96f6-08ceaaea0d41	9159027d-2903-43a9-84b2-726b8196c34c	698eae55140aed8bcbcce165b70d7c02253f80684faaf1a6cd818542c13ab92a	2026-12-05 15:28:15.226458+00	\N	\N	device-1233	2026-11-05 15:28:15.247734+00	2026-11-05 15:28:15.247734+00	0
cd659ae8-6a0e-4f3e-8b89-fcee7d13a6e1	9159027d-2903-43a9-84b2-726b8196c34c	fc39f2461704a7ffb772643f2f6032e6b79d6e22013a592b154d138c59d35595	2026-12-05 15:52:27.900155+00	\N	\N	device-1233	2026-11-05 15:52:28.002111+00	2026-11-05 15:52:28.002111+00	0
b0cf2c4c-3205-4736-81e8-0ddce9b5f27d	9159027d-2903-43a9-84b2-726b8196c34c	225d5596c2a69549fe79035b5e3b3476dc88aa94417d4e640d49336c3a0dc9b9	2026-12-05 16:31:41.122743+00	\N	\N	device-1233	2026-11-05 16:31:41.210016+00	2026-11-05 16:31:41.210016+00	0
7fc67b39-4526-4a40-a59e-c9fa9f244d17	9f8a3d65-8c96-4667-8064-87b077f1f505	5b1c69b53d841bd27917e4cce5ca892f6f1415452fb05b332f4a8e6984b696e0	2026-12-06 11:02:48.33638+00	\N	\N	local	2026-11-06 11:02:48.959823+00	2026-11-06 11:02:48.959823+00	0
446429dc-7ab9-4aff-9aff-cc6a24159c92	9f8a3d65-8c96-4667-8064-87b077f1f505	cef1aa099f61175f00b59600432fbf46e5eba15c96977b3b2083087c5177b490	2026-12-06 11:11:23.102383+00	\N	\N	local	2026-11-06 11:11:23.166578+00	2026-11-06 11:11:23.166578+00	0
b11216d0-91ab-4028-b451-ad3f5afd2996	9159027d-2903-43a9-84b2-726b8196c34c	b829db834c81ce82545cebb98f198e2e8a0876b4b4f5d904e401ef287a1474ea	2026-12-06 11:15:07.137259+00	\N	\N	device-1233	2026-11-06 11:15:07.171106+00	2026-11-06 11:15:07.171106+00	0
db594e95-fa8f-439b-9fd7-dfea1a1079aa	9f8a3d65-8c96-4667-8064-87b077f1f505	becf3fdd0ee21e92be141f59aa4ee54b152086b0ec8a3c638e24e22da6142997	2026-12-06 11:19:38.692262+00	\N	\N	local	2026-11-06 11:19:38.718845+00	2026-11-06 11:19:38.718845+00	0
9bdf3bea-7b72-445f-8045-7f86c1761d37	9f8a3d65-8c96-4667-8064-87b077f1f505	cd71970a4c71c9051698d54ea38a1df46a2698bcc79b12a06102179754fecf68	2026-12-06 12:24:39.257737+00	\N	\N	local	2026-11-06 12:24:39.353609+00	2026-11-06 12:24:39.353609+00	0
79b31aa4-cd07-424d-a190-57b28a74f79f	9f8a3d65-8c96-4667-8064-87b077f1f505	f109fce64b92e9f4dd9ea57530badc4c42d387bf71cdd16bdc8e215734a3751b	2026-12-06 15:39:54.623641+00	\N	\N	local	2026-11-06 15:39:54.703381+00	2026-11-06 15:39:54.703381+00	0
36eefb3e-b9de-46ef-94fc-0c733fbc5389	9159027d-2903-43a9-84b2-726b8196c34c	6b9748643ad04b25a668440bed90bff3dab0cbbc61ad823f993c5f3b27a819ff	2026-12-06 16:55:41.488907+00	\N	\N	123456	2026-11-06 16:55:41.541004+00	2026-11-06 16:55:41.541004+00	0
f4ef6711-56f5-4b79-9533-29f718ba94e7	9f8a3d65-8c96-4667-8064-87b077f1f505	f968db1f78a4282959c8c0cbdbadf4c7aacc718cb2780961338037238b21d92c	2026-12-06 16:58:12.2321+00	\N	\N	local	2026-11-06 16:58:12.260538+00	2026-11-06 16:58:12.260538+00	0
e5dc93c1-9e77-4506-ba77-17aa353f75a3	bc1b2b1c-c1f9-4ee9-8d8b-cceba10dda16	de71a3b3f8ee901fd18bfd32107896886d666a40071ba6df9abc355cb0b544ec	2026-12-06 17:14:35.032747+00	\N	\N	123456842	2026-11-06 17:14:35.13606+00	2026-11-06 17:14:35.13606+00	0
047f0eb7-c168-4d4d-a4fd-16ea4387aabe	bc1b2b1c-c1f9-4ee9-8d8b-cceba10dda16	ccde8cd3259709c9f400cf30fc0d17f310bd4a6c7c8c62b22d2630444f2a8dae	2026-12-06 17:45:42.538053+00	\N	\N	123456842	2026-11-06 17:45:42.571515+00	2026-11-06 17:45:42.571515+00	0
18d3390a-2d46-4003-9778-74576e61ac79	bc1b2b1c-c1f9-4ee9-8d8b-cceba10dda16	ba092c669cc082422e8298bbeadd1830b33bda325b42556c9157c04ffdde9dd9	2026-12-06 19:02:43.153166+00	\N	\N	123456842	2026-11-06 19:02:43.256696+00	2026-11-06 19:02:43.256696+00	0
2744030f-606d-4f88-8a66-054083f95d1f	9f8a3d65-8c96-4667-8064-87b077f1f505	cf709031270c8bab0a9f553f4466c0f0997a5f947216c8bc7e2fb03aeca99a5b	2026-12-06 19:28:01.994866+00	\N	\N	local	2026-11-06 19:28:02.065909+00	2026-11-06 19:28:02.065909+00	0
2d21e1cc-55af-48d6-beb1-e81d3a93d511	bc1b2b1c-c1f9-4ee9-8d8b-cceba10dda16	a25ad840978068332a1b26c934c76f66e81dc6aa26502db13f76ecdc6c090230	2026-12-06 19:28:39.489661+00	\N	\N	123456842	2026-11-06 19:28:39.554043+00	2026-11-06 19:28:39.554043+00	0
ffe1e6c7-8b94-42c5-9242-b3a1e487d899	5e5b2859-2a87-4d44-a05a-c0c3777c4125	d9a3867be28d015d5c602f557134f8f8fea45d9262f2db30eb9d5281f4c9e860	2026-12-06 19:31:09.225677+00	\N	\N	245121	2026-11-06 19:31:09.259319+00	2026-11-06 19:31:09.259319+00	0
743a33df-9643-4da3-a642-6a69056c927e	5e5b2859-2a87-4d44-a05a-c0c3777c4125	c299831c3e11ee7f60fe7761f835a1b6b3ef43c0fe0dd7b194451e3d291cdb51	2026-12-06 19:32:03.161671+00	\N	\N	123456	2026-11-06 19:32:03.177993+00	2026-11-06 19:32:03.177993+00	0
eb35a58d-33d7-4ee2-81bc-329d430342ed	bc1b2b1c-c1f9-4ee9-8d8b-cceba10dda16	153681adcecb1e82818547250cd10c1c54b1c2b58de95f457adba3f7859e266b	2026-12-06 20:17:14.357966+00	\N	\N	123456842	2026-11-06 20:17:14.776821+00	2026-11-06 20:17:14.776821+00	0
ed4d8f2b-74be-43c9-b3b1-405f60932d89	9f8a3d65-8c96-4667-8064-87b077f1f505	114bcc1179805dc7f4028aeb60c46a2f6d17c228805f9afa1fe87ed74f3b6341	2026-12-09 14:15:11.449373+00	\N	\N	web-ec0b052a-f9dc-47b9-ba03-40011dc28654	2026-11-09 14:15:11.507781+00	2026-11-09 14:15:11.507781+00	0
ce74df94-eeea-4b29-99df-78ca4f2752b9	bc1b2b1c-c1f9-4ee9-8d8b-cceba10dda16	6e971b1497c760e283b89aa5579214bed734300535b40facbc0ff8d448648ef0	2026-12-10 22:39:58.254525+00	\N	\N	123456842	2026-11-10 22:39:58.339666+00	2026-11-10 22:39:58.339666+00	0
103e5a94-c997-4f4e-91c9-4a84500003e2	eda23be8-4d83-4178-a5d9-294f5cb90a6f	20a1d5a01fb73306e09cfc390e0b1c649977a9af3d8238436295a7ea8c401525	2026-12-10 22:53:25.790748+00	\N	\N	123456	2026-11-10 22:53:25.816056+00	2026-11-10 22:53:25.816056+00	0
ef8dc2d7-26c3-448e-aac8-62ecafd2d460	9f8a3d65-8c96-4667-8064-87b077f1f505	22f0fb543f966f4bd317f23e4d33c78a2d9bf82f083dae1f7e23d95d5ae0b2ad	2026-12-10 23:49:33.543827+00	\N	\N	local	2026-11-10 23:49:33.601994+00	2026-11-10 23:49:33.601994+00	0
f3321695-ccf1-479c-a7ba-74bc92dda379	9f8a3d65-8c96-4667-8064-87b077f1f505	a5f72789aecdf82c49e36adb9709d69ef7c37f9fac36a35b515bbe4fed232017	2026-12-11 13:24:28.57947+00	\N	\N	local	2026-11-11 13:24:28.666184+00	2026-11-11 13:24:28.666184+00	0
010ab3c9-75b6-425b-8e07-4b4853d147a9	6496523f-63c7-4a39-b8db-8ae492981b19	bba7fc5711069ef93d18468b181a1090f657a07773bca47d6115d2f42c977696	2026-12-11 13:27:50.415067+00	\N	\N	123456	2026-11-11 13:27:50.473438+00	2026-11-11 13:27:50.473438+00	0
01383873-f5c9-4bee-afc5-bb2a0aaa14dd	6496523f-63c7-4a39-b8db-8ae492981b19	3e1668e5a7a645f41a317b602375f457b91f1f82a79589ceaa9dcdc69884cdeb	2026-12-11 13:33:17.728979+00	\N	\N	123456	2026-11-11 13:33:17.747841+00	2026-11-11 13:33:17.747841+00	0
65a9b8b2-9e28-4bbf-87c4-7c67ccec40de	6496523f-63c7-4a39-b8db-8ae492981b19	ddf6ccd2b0bda60b936b68cd975e8dd757f939740c4c2d0e55ed96d1639c00da	2026-12-11 13:50:16.236999+00	\N	\N	123456	2026-11-11 13:50:16.286714+00	2026-11-11 13:50:16.286714+00	0
30c61e75-8c40-4e40-ad10-83f4b91cb4cd	cedc8f2f-f2ba-42fe-a03b-de4ff1ff2f56	ccd09350efeda27dd2340dc8828d6bdcdaf2b01600a42c82f8cd8e1781d816d7	2026-12-11 13:51:39.017578+00	\N	\N	123456	2026-11-11 13:51:39.038562+00	2026-11-11 13:51:39.038562+00	0
9f6e2b9f-8624-4200-a5c6-bb3b063ccf2e	cedc8f2f-f2ba-42fe-a03b-de4ff1ff2f56	674951d0e9cc3c4b6d737180222aff00ef8eae95746550b6f98989cbf6c8455b	2026-12-11 13:52:11.593074+00	\N	\N	123456	2026-11-11 13:52:11.606103+00	2026-11-11 13:52:11.606103+00	0
9f3077b4-4042-479e-96cf-96e1394a80cd	cedc8f2f-f2ba-42fe-a03b-de4ff1ff2f56	aba4ad7a168a34b44ea3db6b102a2bfb44056eb72b1c4d7213a2c234dd06b800	2026-12-11 14:07:33.528976+00	\N	\N	123456	2026-11-11 14:07:33.5996+00	2026-11-11 14:07:33.5996+00	0
5d817013-d549-4507-851f-e52c880b63bf	6496523f-63c7-4a39-b8db-8ae492981b19	cfae3419e41c5dd4c236c0c0ed46eadfdb72752c53d33f7de1642b614c5cc0c5	2026-12-11 14:08:09.752817+00	\N	\N	123456	2026-11-11 14:08:09.769255+00	2026-11-11 14:08:09.769255+00	0
f73c6e8b-c683-4f49-83ec-e326d4e91206	cedc8f2f-f2ba-42fe-a03b-de4ff1ff2f56	999a0cc49d5011c6f1816735d696aa59412be111b775aeaca8b423d06e2e8f54	2026-12-11 14:15:15.220916+00	\N	\N	123456	2026-11-11 14:15:15.266683+00	2026-11-11 14:15:15.266683+00	0
ed2a7b6d-c58d-4748-89de-b4db0b8d73ef	6496523f-63c7-4a39-b8db-8ae492981b19	353c30d83322dba3dc74f981fd616aa085b4ba1858ae83a8719ccab5bcf52e6d	2026-12-11 14:28:18.896056+00	\N	\N	123456	2026-11-11 14:28:18.938793+00	2026-11-11 14:28:18.938793+00	0
\.


--
-- Data for Name: staff_invitation; Type: TABLE DATA; Schema: identity; Owner: postgres
--

COPY identity.staff_invitation (id, clinic_id, phone, role, token_hash, invited_by_user_id, status, expires_at, accepted_at, created_at, updated_at, version) FROM stdin;
e213694d-3691-4970-9f11-81129490afdc	9039cc7c-ac66-4adc-a443-f5cd3f213b57	+201020061562	DOCTOR	c077f53bfd6c44f8acffb485c12f308c35cb5141a4d4c76685660ed679ec7042	9159027d-2903-43a9-84b2-726b8196c34c	PENDING	2026-11-07 15:39:59.00951+00	\N	2026-11-04 15:39:59.23223+00	2026-11-04 15:39:59.263744+00	1
46add768-58d3-41cf-b2f9-9d7e4890b73f	9039cc7c-ac66-4adc-a443-f5cd3f213b57	+201020061563	DOCTOR	ac0e416c72f73e1adc3fb41b3658c25a345a1151dc531804590895d4edded835	9159027d-2903-43a9-84b2-726b8196c34c	ACCEPTED	2026-11-08 15:30:12.416467+00	2026-11-05 15:34:29.952365+00	2026-11-05 15:30:12.466933+00	2026-11-05 15:34:31.616032+00	2
\.


--
-- Data for Name: staff_membership; Type: TABLE DATA; Schema: identity; Owner: postgres
--

COPY identity.staff_membership (id, user_id, clinic_id, role, status, created_at, updated_at, version) FROM stdin;
ea406fef-057c-4daa-8bb3-79759596a8cd	9159027d-2903-43a9-84b2-726b8196c34c	9039cc7c-ac66-4adc-a443-f5cd3f213b57	OWNER	ACTIVE	2026-11-04 15:38:54.121856+00	2026-11-04 15:38:54.121856+00	0
9ad29e57-b525-4b60-ba4c-47e0e57d9a69	e913de7f-49b5-4ae9-9627-3cf55d9b5982	9039cc7c-ac66-4adc-a443-f5cd3f213b57	DOCTOR	ACTIVE	2026-11-05 15:34:31.353423+00	2026-11-05 15:34:31.385864+00	1
df55b114-faed-4868-95b9-ffc1c73f4111	5cc50af5-a72e-4bce-b23f-392ec44767b1	3fa85f64-5717-4562-b3fc-2c963f66afa6	DOCTOR	ACTIVE	2026-11-06 11:12:37.222899+00	2026-11-06 11:12:37.222899+00	0
b65d601d-8d43-4291-8330-ba4bd6a73088	b1230650-5dd9-4500-bbf0-ffb4eec55888	3fa85f64-5717-4562-b3fc-2c963f66afb6	DOCTOR	ACTIVE	2026-11-06 11:21:01.737174+00	2026-11-06 11:21:01.737174+00	0
60f262b8-d2c2-4396-9bd8-b3320221ee3e	d8aedf8b-1783-4b08-8c12-c537f5a53524	9039cc7c-ac66-4adc-a443-f5cd3f213b57	DOCTOR	ACTIVE	2026-11-06 12:29:31.399595+00	2026-11-06 12:29:31.399595+00	0
35323bfe-21eb-4fe5-97b8-99e1dab2790b	c18e5dff-ba0d-4d81-8b36-508304b222da	9039cc7c-ac66-4adc-a443-f5cd3f213b57	DOCTOR	ACTIVE	2026-11-06 15:48:57.836251+00	2026-11-06 15:48:57.836251+00	0
5b9213a8-8afe-4093-a629-f5e4c5ca0c1d	038b411f-27c5-42af-b297-32ef1e5ec929	9039cc7c-ac66-4adc-a443-f5cd3f213b57	DOCTOR	ACTIVE	2026-11-06 17:05:44.748892+00	2026-11-06 17:05:44.748892+00	0
ce501cf2-4b0a-42ff-a0c5-2cbc83e70705	9159027d-2903-43a9-84b2-726b8196c34c	4e02b20e-0311-49c8-90ca-b1069382ff6e	OWNER	ACTIVE	2026-11-06 17:10:22.842178+00	2026-11-06 17:10:22.842178+00	0
4b2d570c-7355-4f84-94d6-73bd4f5b9fe6	bc1b2b1c-c1f9-4ee9-8d8b-cceba10dda16	fb420eb5-dcc2-4eb0-af42-7a1ffdcf7b3f	OWNER	ACTIVE	2026-11-06 17:16:24.859461+00	2026-11-06 17:16:24.859461+00	0
6ed01fa9-1344-47ae-a572-99740e4c5a0b	d8aedf8b-1783-4b08-8c12-c537f5a53524	fb420eb5-dcc2-4eb0-af42-7a1ffdcf7b3f	DOCTOR	ACTIVE	2026-11-06 19:03:12.011166+00	2026-11-06 19:03:12.011166+00	0
a49c01af-ac7b-4932-9761-04b36ea0d320	5e5b2859-2a87-4d44-a05a-c0c3777c4125	fb420eb5-dcc2-4eb0-af42-7a1ffdcf7b3f	DOCTOR	ACTIVE	2026-11-06 19:30:19.266274+00	2026-11-06 19:30:19.266274+00	0
24f7da3a-9eea-4d1a-8751-749ebe1f44a9	26d1c678-9e5d-441c-b2e2-fd9147fa33fc	fb420eb5-dcc2-4eb0-af42-7a1ffdcf7b3f	DOCTOR	ACTIVE	2026-11-09 14:17:18.314296+00	2026-11-09 14:17:18.314296+00	0
1ea33508-e120-4d23-b01e-4e954f2b87ae	eda23be8-4d83-4178-a5d9-294f5cb90a6f	fb420eb5-dcc2-4eb0-af42-7a1ffdcf7b3f	DOCTOR	ACTIVE	2026-11-10 22:53:25.150202+00	2026-11-10 22:53:25.150202+00	0
b830dac0-7a52-458b-bed8-0418ce615be9	6496523f-63c7-4a39-b8db-8ae492981b19	5e5fdfca-7e79-4d6d-838d-1b938d07a9e4	OWNER	ACTIVE	2026-11-11 13:28:39.455309+00	2026-11-11 13:28:39.455309+00	0
bf1c9f96-b172-47fd-905f-3eff4d0999d6	cedc8f2f-f2ba-42fe-a03b-de4ff1ff2f56	5e5fdfca-7e79-4d6d-838d-1b938d07a9e4	DOCTOR	ACTIVE	2026-11-11 13:51:38.792441+00	2026-11-11 13:51:38.792441+00	0
\.


--
-- Data for Name: admin_action_audit; Type: TABLE DATA; Schema: platform_admin; Owner: postgres
--

COPY platform_admin.admin_action_audit (id, actor_user_id, action_type, target_type, target_id, previous_value, new_value, reason, created_at) FROM stdin;
\.


--
-- Data for Name: admin_grant; Type: TABLE DATA; Schema: platform_admin; Owner: postgres
--

COPY platform_admin.admin_grant (id, user_id, status, granted_by, revoked_by, revoked_at, created_at, updated_at, version) FROM stdin;
\.


--
-- Data for Name: admin_grant_permission; Type: TABLE DATA; Schema: platform_admin; Owner: postgres
--

COPY platform_admin.admin_grant_permission (grant_id, permission) FROM stdin;
\.


--
-- Data for Name: event_publication; Type: TABLE DATA; Schema: public; Owner: postgres
--

COPY public.event_publication (id, completion_attempts, completion_date, event_type, last_resubmission_date, listener_id, publication_date, serialized_event, status) FROM stdin;
\.


--
-- Data for Name: flyway_schema_history; Type: TABLE DATA; Schema: public; Owner: postgres
--

COPY public.flyway_schema_history (installed_rank, version, description, type, script, checksum, installed_by, installed_on, execution_time, success) FROM stdin;
1	1	create identity module	SQL	V1__create_identity_module.sql	1717208192	postgres	2026-11-04 15:20:55.945956	339	t
2	2	create clinic doctor and relationship modules	SQL	V2__create_clinic_doctor_and_relationship_modules.sql	-774499583	postgres	2026-11-04 15:20:56.340002	206	t
3	3	create modulith event publication	SQL	V3__create_modulith_event_publication.sql	-455698692	postgres	2026-11-04 15:30:53.455845	121	t
4	4	add system role and admin bootstrap support	SQL	V4__add_system_role_and_admin_bootstrap_support.sql	-1553242991	postgres	2026-11-05 18:14:25.437109	3118	t
5	5	platform admin subscription review and seed repair	SQL	V5__platform_admin_subscription_review_and_seed_repair.sql	-1608295309	postgres	2026-11-06 01:04:56.985923	603	t
6	6	doctor onboarding and password rotation	SQL	V6__doctor_onboarding_and_password_rotation.sql	900444540	postgres	2026-11-06 16:08:25.691482	142	t
7	7	reconcile lightweight doctor onboarding invite	SQL	V7__reconcile_lightweight_doctor_onboarding_invite.sql	-1337934759	postgres	2026-11-06 18:31:46.060891	303	t
8	8	clinic verification workflow	SQL	V8__clinic_verification_workflow.sql	1370380212	postgres	2026-11-09 16:12:31.136459	132	t
9	9	create scheduling and booking sessions	SQL	V9__create_scheduling_and_booking_sessions.sql	368768599	postgres	2026-11-11 15:05:03.370729	222	t
\.


--
-- Data for Name: clinic_review; Type: TABLE DATA; Schema: review; Owner: postgres
--

COPY review.clinic_review (id, clinic_id, doctor_id, patient_user_id, rating, comment, status, moderation_reason, moderated_by, moderated_at, created_at, updated_at, version) FROM stdin;
\.


--
-- Data for Name: review_moderation_audit; Type: TABLE DATA; Schema: review; Owner: postgres
--

COPY review.review_moderation_audit (id, review_id, actor_user_id, action_type, previous_status, new_status, reason, created_at) FROM stdin;
\.


--
-- Data for Name: schedule_exception; Type: TABLE DATA; Schema: scheduling; Owner: postgres
--

COPY scheduling.schedule_exception (id, doctor_clinic_id, shift_id, type, date_from, date_to, changed_start_time, changed_end_time, reason, cancelled_at, created_at, updated_at, version) FROM stdin;
\.


--
-- Data for Name: schedule_guard; Type: TABLE DATA; Schema: scheduling; Owner: postgres
--

COPY scheduling.schedule_guard (doctor_clinic_id, updated_at) FROM stdin;
f13f11ba-9379-4871-b77d-6fcd3b19be61	2026-11-11 13:59:36.890107+00
381699f4-32e6-4a3c-ba24-dc0a1fe5bff3	2026-11-11 14:05:00.49229+00
\.


--
-- Data for Name: shift; Type: TABLE DATA; Schema: scheduling; Owner: postgres
--

COPY scheduling.shift (id, doctor_clinic_id, name, capacity_mode, average_visit_minutes, fixed_capacity, effective_from, active, created_at, updated_at, version) FROM stdin;
f5c6c8b7-dc86-4b0f-91a1-60d57a7c5b46	f13f11ba-9379-4871-b77d-6fcd3b19be61	Morning clinic	CALCULATED	15	\N	2026-11-11	t	2026-11-11 13:59:37.541604+00	2026-11-11 13:59:37.541604+00	0
\.


--
-- Data for Name: shift_break; Type: TABLE DATA; Schema: scheduling; Owner: postgres
--

COPY scheduling.shift_break (id, rule_id, start_time, end_time) FROM stdin;
e0a8a42c-b01f-45e3-8682-58d2145fcad4	eed46bb3-caad-491b-a35a-7fd3c05c6a3a	11:00:00	11:15:00
\.


--
-- Data for Name: weekly_rule; Type: TABLE DATA; Schema: scheduling; Owner: postgres
--

COPY scheduling.weekly_rule (id, shift_id, day_of_week, start_time, end_time, follow_up_quota) FROM stdin;
eed46bb3-caad-491b-a35a-7fd3c05c6a3a	f5c6c8b7-dc86-4b0f-91a1-60d57a7c5b46	SUNDAY	09:00:00	14:00:00	2
\.


--
-- Data for Name: clinic_subscription; Type: TABLE DATA; Schema: subscription; Owner: postgres
--

COPY subscription.clinic_subscription (id, clinic_id, plan_id, status, starts_at, ends_at, activated_by, cancelled_by, reason, created_at, updated_at, version) FROM stdin;
\.


--
-- Data for Name: plan; Type: TABLE DATA; Schema: subscription; Owner: postgres
--

COPY subscription.plan (id, code, name, duration_days, price_amount, currency, clinic_limit, doctor_limit, active, created_at, updated_at, version) FROM stdin;
\.


--
-- Name: session session_pkey; Type: CONSTRAINT; Schema: booking; Owner: postgres
--

ALTER TABLE ONLY booking.session
    ADD CONSTRAINT session_pkey PRIMARY KEY (id);


--
-- Name: session uk_booking_session_slot; Type: CONSTRAINT; Schema: booking; Owner: postgres
--

ALTER TABLE ONLY booking.session
    ADD CONSTRAINT uk_booking_session_slot UNIQUE (doctor_clinic_id, shift_id, session_date);


--
-- Name: area area_pkey; Type: CONSTRAINT; Schema: clinic; Owner: postgres
--

ALTER TABLE ONLY clinic.area
    ADD CONSTRAINT area_pkey PRIMARY KEY (id);


--
-- Name: clinic clinic_pkey; Type: CONSTRAINT; Schema: clinic; Owner: postgres
--

ALTER TABLE ONLY clinic.clinic
    ADD CONSTRAINT clinic_pkey PRIMARY KEY (id);


--
-- Name: create_idempotency pk_clinic_create_idempotency; Type: CONSTRAINT; Schema: clinic; Owner: postgres
--

ALTER TABLE ONLY clinic.create_idempotency
    ADD CONSTRAINT pk_clinic_create_idempotency PRIMARY KEY (actor_user_id, idempotency_key);


--
-- Name: clinic_phone pk_clinic_phone; Type: CONSTRAINT; Schema: clinic; Owner: postgres
--

ALTER TABLE ONLY clinic.clinic_phone
    ADD CONSTRAINT pk_clinic_phone PRIMARY KEY (clinic_id, phone);


--
-- Name: area uk_clinic_area_code; Type: CONSTRAINT; Schema: clinic; Owner: postgres
--

ALTER TABLE ONLY clinic.area
    ADD CONSTRAINT uk_clinic_area_code UNIQUE (code);


--
-- Name: verification_event verification_event_pkey; Type: CONSTRAINT; Schema: clinic; Owner: postgres
--

ALTER TABLE ONLY clinic.verification_event
    ADD CONSTRAINT verification_event_pkey PRIMARY KEY (id);


--
-- Name: doctor_profile doctor_profile_pkey; Type: CONSTRAINT; Schema: doctor; Owner: postgres
--

ALTER TABLE ONLY doctor.doctor_profile
    ADD CONSTRAINT doctor_profile_pkey PRIMARY KEY (id);


--
-- Name: doctor_specialty pk_doctor_specialty; Type: CONSTRAINT; Schema: doctor; Owner: postgres
--

ALTER TABLE ONLY doctor.doctor_specialty
    ADD CONSTRAINT pk_doctor_specialty PRIMARY KEY (doctor_id, specialty_id);


--
-- Name: specialty specialty_pkey; Type: CONSTRAINT; Schema: doctor; Owner: postgres
--

ALTER TABLE ONLY doctor.specialty
    ADD CONSTRAINT specialty_pkey PRIMARY KEY (id);


--
-- Name: doctor_profile uk_doctor_profile_user; Type: CONSTRAINT; Schema: doctor; Owner: postgres
--

ALTER TABLE ONLY doctor.doctor_profile
    ADD CONSTRAINT uk_doctor_profile_user UNIQUE (user_id);


--
-- Name: specialty uk_doctor_specialty_code; Type: CONSTRAINT; Schema: doctor; Owner: postgres
--

ALTER TABLE ONLY doctor.specialty
    ADD CONSTRAINT uk_doctor_specialty_code UNIQUE (code);


--
-- Name: doctor_clinic doctor_clinic_pkey; Type: CONSTRAINT; Schema: doctor_clinic; Owner: postgres
--

ALTER TABLE ONLY doctor_clinic.doctor_clinic
    ADD CONSTRAINT doctor_clinic_pkey PRIMARY KEY (id);


--
-- Name: doctor_clinic uk_doctor_clinic_pair; Type: CONSTRAINT; Schema: doctor_clinic; Owner: postgres
--

ALTER TABLE ONLY doctor_clinic.doctor_clinic
    ADD CONSTRAINT uk_doctor_clinic_pair UNIQUE (clinic_id, doctor_id);


--
-- Name: app_user app_user_pkey; Type: CONSTRAINT; Schema: identity; Owner: postgres
--

ALTER TABLE ONLY identity.app_user
    ADD CONSTRAINT app_user_pkey PRIMARY KEY (id);


--
-- Name: doctor_onboarding_invite doctor_onboarding_invite_pkey; Type: CONSTRAINT; Schema: identity; Owner: postgres
--

ALTER TABLE ONLY identity.doctor_onboarding_invite
    ADD CONSTRAINT doctor_onboarding_invite_pkey PRIMARY KEY (id);


--
-- Name: otp_challenge otp_challenge_pkey; Type: CONSTRAINT; Schema: identity; Owner: postgres
--

ALTER TABLE ONLY identity.otp_challenge
    ADD CONSTRAINT otp_challenge_pkey PRIMARY KEY (id);


--
-- Name: password_login_attempt password_login_attempt_pkey; Type: CONSTRAINT; Schema: identity; Owner: postgres
--

ALTER TABLE ONLY identity.password_login_attempt
    ADD CONSTRAINT password_login_attempt_pkey PRIMARY KEY (phone);


--
-- Name: invitation_permission pk_identity_invitation_permission; Type: CONSTRAINT; Schema: identity; Owner: postgres
--

ALTER TABLE ONLY identity.invitation_permission
    ADD CONSTRAINT pk_identity_invitation_permission PRIMARY KEY (invitation_id, permission);


--
-- Name: membership_permission pk_identity_membership_permission; Type: CONSTRAINT; Schema: identity; Owner: postgres
--

ALTER TABLE ONLY identity.membership_permission
    ADD CONSTRAINT pk_identity_membership_permission PRIMARY KEY (membership_id, permission);


--
-- Name: refresh_session refresh_session_pkey; Type: CONSTRAINT; Schema: identity; Owner: postgres
--

ALTER TABLE ONLY identity.refresh_session
    ADD CONSTRAINT refresh_session_pkey PRIMARY KEY (id);


--
-- Name: staff_invitation staff_invitation_pkey; Type: CONSTRAINT; Schema: identity; Owner: postgres
--

ALTER TABLE ONLY identity.staff_invitation
    ADD CONSTRAINT staff_invitation_pkey PRIMARY KEY (id);


--
-- Name: staff_membership staff_membership_pkey; Type: CONSTRAINT; Schema: identity; Owner: postgres
--

ALTER TABLE ONLY identity.staff_membership
    ADD CONSTRAINT staff_membership_pkey PRIMARY KEY (id);


--
-- Name: doctor_onboarding_invite uk_identity_doc_invite_token_hash; Type: CONSTRAINT; Schema: identity; Owner: postgres
--

ALTER TABLE ONLY identity.doctor_onboarding_invite
    ADD CONSTRAINT uk_identity_doc_invite_token_hash UNIQUE (token_hash);


--
-- Name: staff_invitation uk_identity_invitation_token_hash; Type: CONSTRAINT; Schema: identity; Owner: postgres
--

ALTER TABLE ONLY identity.staff_invitation
    ADD CONSTRAINT uk_identity_invitation_token_hash UNIQUE (token_hash);


--
-- Name: staff_membership uk_identity_membership_clinic_user; Type: CONSTRAINT; Schema: identity; Owner: postgres
--

ALTER TABLE ONLY identity.staff_membership
    ADD CONSTRAINT uk_identity_membership_clinic_user UNIQUE (clinic_id, user_id);


--
-- Name: otp_challenge uk_identity_otp_verification_token; Type: CONSTRAINT; Schema: identity; Owner: postgres
--

ALTER TABLE ONLY identity.otp_challenge
    ADD CONSTRAINT uk_identity_otp_verification_token UNIQUE (verified_token_hash);


--
-- Name: refresh_session uk_identity_refresh_token_hash; Type: CONSTRAINT; Schema: identity; Owner: postgres
--

ALTER TABLE ONLY identity.refresh_session
    ADD CONSTRAINT uk_identity_refresh_token_hash UNIQUE (token_hash);


--
-- Name: app_user uk_identity_user_phone; Type: CONSTRAINT; Schema: identity; Owner: postgres
--

ALTER TABLE ONLY identity.app_user
    ADD CONSTRAINT uk_identity_user_phone UNIQUE (phone);


--
-- Name: admin_action_audit admin_action_audit_pkey; Type: CONSTRAINT; Schema: platform_admin; Owner: postgres
--

ALTER TABLE ONLY platform_admin.admin_action_audit
    ADD CONSTRAINT admin_action_audit_pkey PRIMARY KEY (id);


--
-- Name: admin_grant admin_grant_pkey; Type: CONSTRAINT; Schema: platform_admin; Owner: postgres
--

ALTER TABLE ONLY platform_admin.admin_grant
    ADD CONSTRAINT admin_grant_pkey PRIMARY KEY (id);


--
-- Name: admin_grant_permission pk_platform_admin_grant_permission; Type: CONSTRAINT; Schema: platform_admin; Owner: postgres
--

ALTER TABLE ONLY platform_admin.admin_grant_permission
    ADD CONSTRAINT pk_platform_admin_grant_permission PRIMARY KEY (grant_id, permission);


--
-- Name: admin_grant uk_platform_admin_grant_user; Type: CONSTRAINT; Schema: platform_admin; Owner: postgres
--

ALTER TABLE ONLY platform_admin.admin_grant
    ADD CONSTRAINT uk_platform_admin_grant_user UNIQUE (user_id);


--
-- Name: event_publication event_publication_pkey; Type: CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.event_publication
    ADD CONSTRAINT event_publication_pkey PRIMARY KEY (id);


--
-- Name: flyway_schema_history flyway_schema_history_pk; Type: CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.flyway_schema_history
    ADD CONSTRAINT flyway_schema_history_pk PRIMARY KEY (installed_rank);


--
-- Name: clinic_review clinic_review_pkey; Type: CONSTRAINT; Schema: review; Owner: postgres
--

ALTER TABLE ONLY review.clinic_review
    ADD CONSTRAINT clinic_review_pkey PRIMARY KEY (id);


--
-- Name: review_moderation_audit review_moderation_audit_pkey; Type: CONSTRAINT; Schema: review; Owner: postgres
--

ALTER TABLE ONLY review.review_moderation_audit
    ADD CONSTRAINT review_moderation_audit_pkey PRIMARY KEY (id);


--
-- Name: schedule_exception schedule_exception_pkey; Type: CONSTRAINT; Schema: scheduling; Owner: postgres
--

ALTER TABLE ONLY scheduling.schedule_exception
    ADD CONSTRAINT schedule_exception_pkey PRIMARY KEY (id);


--
-- Name: schedule_guard schedule_guard_pkey; Type: CONSTRAINT; Schema: scheduling; Owner: postgres
--

ALTER TABLE ONLY scheduling.schedule_guard
    ADD CONSTRAINT schedule_guard_pkey PRIMARY KEY (doctor_clinic_id);


--
-- Name: shift_break shift_break_pkey; Type: CONSTRAINT; Schema: scheduling; Owner: postgres
--

ALTER TABLE ONLY scheduling.shift_break
    ADD CONSTRAINT shift_break_pkey PRIMARY KEY (id);


--
-- Name: shift shift_pkey; Type: CONSTRAINT; Schema: scheduling; Owner: postgres
--

ALTER TABLE ONLY scheduling.shift
    ADD CONSTRAINT shift_pkey PRIMARY KEY (id);


--
-- Name: weekly_rule uk_scheduling_weekly_rule_day; Type: CONSTRAINT; Schema: scheduling; Owner: postgres
--

ALTER TABLE ONLY scheduling.weekly_rule
    ADD CONSTRAINT uk_scheduling_weekly_rule_day UNIQUE (shift_id, day_of_week);


--
-- Name: weekly_rule weekly_rule_pkey; Type: CONSTRAINT; Schema: scheduling; Owner: postgres
--

ALTER TABLE ONLY scheduling.weekly_rule
    ADD CONSTRAINT weekly_rule_pkey PRIMARY KEY (id);


--
-- Name: clinic_subscription clinic_subscription_pkey; Type: CONSTRAINT; Schema: subscription; Owner: postgres
--

ALTER TABLE ONLY subscription.clinic_subscription
    ADD CONSTRAINT clinic_subscription_pkey PRIMARY KEY (id);


--
-- Name: plan plan_pkey; Type: CONSTRAINT; Schema: subscription; Owner: postgres
--

ALTER TABLE ONLY subscription.plan
    ADD CONSTRAINT plan_pkey PRIMARY KEY (id);


--
-- Name: plan uk_subscription_plan_code; Type: CONSTRAINT; Schema: subscription; Owner: postgres
--

ALTER TABLE ONLY subscription.plan
    ADD CONSTRAINT uk_subscription_plan_code UNIQUE (code);


--
-- Name: ix_booking_session_doctor_clinic_date; Type: INDEX; Schema: booking; Owner: postgres
--

CREATE INDEX ix_booking_session_doctor_clinic_date ON booking.session USING btree (doctor_clinic_id, session_date, start_time);


--
-- Name: ix_clinic_area_city_active; Type: INDEX; Schema: clinic; Owner: postgres
--

CREATE INDEX ix_clinic_area_city_active ON clinic.area USING btree (city, active, name_en);


--
-- Name: ix_clinic_create_idempotency_created_at; Type: INDEX; Schema: clinic; Owner: postgres
--

CREATE INDEX ix_clinic_create_idempotency_created_at ON clinic.create_idempotency USING btree (created_at);


--
-- Name: ix_clinic_owner; Type: INDEX; Schema: clinic; Owner: postgres
--

CREATE INDEX ix_clinic_owner ON clinic.clinic USING btree (owner_user_id, created_at DESC);


--
-- Name: ix_clinic_status_area; Type: INDEX; Schema: clinic; Owner: postgres
--

CREATE INDEX ix_clinic_status_area ON clinic.clinic USING btree (status, area_id);


--
-- Name: ix_clinic_verification_event_actor; Type: INDEX; Schema: clinic; Owner: postgres
--

CREATE INDEX ix_clinic_verification_event_actor ON clinic.verification_event USING btree (actor_user_id, created_at DESC);


--
-- Name: ix_clinic_verification_event_clinic; Type: INDEX; Schema: clinic; Owner: postgres
--

CREATE INDEX ix_clinic_verification_event_clinic ON clinic.verification_event USING btree (clinic_id, created_at DESC);


--
-- Name: uk_clinic_name_ci; Type: INDEX; Schema: clinic; Owner: postgres
--

CREATE UNIQUE INDEX uk_clinic_name_ci ON clinic.clinic USING btree (lower((name)::text));


--
-- Name: ix_doctor_profile_verification; Type: INDEX; Schema: doctor; Owner: postgres
--

CREATE INDEX ix_doctor_profile_verification ON doctor.doctor_profile USING btree (verification_status, created_at DESC);


--
-- Name: ix_doctor_specialty_specialty; Type: INDEX; Schema: doctor; Owner: postgres
--

CREATE INDEX ix_doctor_specialty_specialty ON doctor.doctor_specialty USING btree (specialty_id, doctor_id);


--
-- Name: uk_doctor_profile_syndicate_number_ci; Type: INDEX; Schema: doctor; Owner: postgres
--

CREATE UNIQUE INDEX uk_doctor_profile_syndicate_number_ci ON doctor.doctor_profile USING btree (lower((syndicate_number)::text)) WHERE (syndicate_number IS NOT NULL);


--
-- Name: ix_doctor_clinic_clinic; Type: INDEX; Schema: doctor_clinic; Owner: postgres
--

CREATE INDEX ix_doctor_clinic_clinic ON doctor_clinic.doctor_clinic USING btree (clinic_id, status, doctor_id);


--
-- Name: ix_doctor_clinic_doctor; Type: INDEX; Schema: doctor_clinic; Owner: postgres
--

CREATE INDEX ix_doctor_clinic_doctor ON doctor_clinic.doctor_clinic USING btree (doctor_id, status, clinic_id);


--
-- Name: ix_identity_doc_invite_clinic_phone; Type: INDEX; Schema: identity; Owner: postgres
--

CREATE INDEX ix_identity_doc_invite_clinic_phone ON identity.doctor_onboarding_invite USING btree (clinic_id, phone, status);


--
-- Name: ix_identity_doc_invite_expiry; Type: INDEX; Schema: identity; Owner: postgres
--

CREATE INDEX ix_identity_doc_invite_expiry ON identity.doctor_onboarding_invite USING btree (status, expires_at);


--
-- Name: ix_identity_doc_invite_phone_created; Type: INDEX; Schema: identity; Owner: postgres
--

CREATE INDEX ix_identity_doc_invite_phone_created ON identity.doctor_onboarding_invite USING btree (phone, created_at DESC);


--
-- Name: ix_identity_invitation_expiry; Type: INDEX; Schema: identity; Owner: postgres
--

CREATE INDEX ix_identity_invitation_expiry ON identity.staff_invitation USING btree (expires_at, status);


--
-- Name: ix_identity_membership_clinic; Type: INDEX; Schema: identity; Owner: postgres
--

CREATE INDEX ix_identity_membership_clinic ON identity.staff_membership USING btree (clinic_id, status, role);


--
-- Name: ix_identity_membership_user; Type: INDEX; Schema: identity; Owner: postgres
--

CREATE INDEX ix_identity_membership_user ON identity.staff_membership USING btree (user_id, status);


--
-- Name: ix_identity_otp_expiry; Type: INDEX; Schema: identity; Owner: postgres
--

CREATE INDEX ix_identity_otp_expiry ON identity.otp_challenge USING btree (expires_at);


--
-- Name: ix_identity_otp_phone_created; Type: INDEX; Schema: identity; Owner: postgres
--

CREATE INDEX ix_identity_otp_phone_created ON identity.otp_challenge USING btree (phone, created_at DESC);


--
-- Name: ix_identity_refresh_user_active; Type: INDEX; Schema: identity; Owner: postgres
--

CREATE INDEX ix_identity_refresh_user_active ON identity.refresh_session USING btree (user_id, expires_at) WHERE (revoked_at IS NULL);


--
-- Name: ix_identity_user_system_role; Type: INDEX; Schema: identity; Owner: postgres
--

CREATE INDEX ix_identity_user_system_role ON identity.app_user USING btree (system_role);


--
-- Name: uk_identity_pending_invitation_clinic_phone; Type: INDEX; Schema: identity; Owner: postgres
--

CREATE UNIQUE INDEX uk_identity_pending_invitation_clinic_phone ON identity.staff_invitation USING btree (clinic_id, phone) WHERE ((status)::text = 'PENDING'::text);


--
-- Name: uk_identity_user_email_not_null; Type: INDEX; Schema: identity; Owner: postgres
--

CREATE UNIQUE INDEX uk_identity_user_email_not_null ON identity.app_user USING btree (lower((email)::text)) WHERE (email IS NOT NULL);


--
-- Name: ix_platform_admin_audit_actor; Type: INDEX; Schema: platform_admin; Owner: postgres
--

CREATE INDEX ix_platform_admin_audit_actor ON platform_admin.admin_action_audit USING btree (actor_user_id, created_at DESC);


--
-- Name: ix_platform_admin_audit_target; Type: INDEX; Schema: platform_admin; Owner: postgres
--

CREATE INDEX ix_platform_admin_audit_target ON platform_admin.admin_action_audit USING btree (target_type, target_id, created_at DESC);


--
-- Name: ix_platform_admin_grant_status; Type: INDEX; Schema: platform_admin; Owner: postgres
--

CREATE INDEX ix_platform_admin_grant_status ON platform_admin.admin_grant USING btree (status);


--
-- Name: flyway_schema_history_s_idx; Type: INDEX; Schema: public; Owner: postgres
--

CREATE INDEX flyway_schema_history_s_idx ON public.flyway_schema_history USING btree (success);


--
-- Name: ix_event_publication_completion_date; Type: INDEX; Schema: public; Owner: postgres
--

CREATE INDEX ix_event_publication_completion_date ON public.event_publication USING btree (completion_date);


--
-- Name: ix_review_clinic; Type: INDEX; Schema: review; Owner: postgres
--

CREATE INDEX ix_review_clinic ON review.clinic_review USING btree (clinic_id, status);


--
-- Name: ix_review_moderation_review; Type: INDEX; Schema: review; Owner: postgres
--

CREATE INDEX ix_review_moderation_review ON review.review_moderation_audit USING btree (review_id, created_at DESC);


--
-- Name: ix_review_status; Type: INDEX; Schema: review; Owner: postgres
--

CREATE INDEX ix_review_status ON review.clinic_review USING btree (status, created_at DESC);


--
-- Name: ix_scheduling_exception_doctor_clinic_dates; Type: INDEX; Schema: scheduling; Owner: postgres
--

CREATE INDEX ix_scheduling_exception_doctor_clinic_dates ON scheduling.schedule_exception USING btree (doctor_clinic_id, date_from, date_to);


--
-- Name: ix_scheduling_shift_break_rule; Type: INDEX; Schema: scheduling; Owner: postgres
--

CREATE INDEX ix_scheduling_shift_break_rule ON scheduling.shift_break USING btree (rule_id);


--
-- Name: ix_scheduling_shift_doctor_clinic_active; Type: INDEX; Schema: scheduling; Owner: postgres
--

CREATE INDEX ix_scheduling_shift_doctor_clinic_active ON scheduling.shift USING btree (doctor_clinic_id, active, effective_from);


--
-- Name: ix_scheduling_weekly_rule_shift; Type: INDEX; Schema: scheduling; Owner: postgres
--

CREATE INDEX ix_scheduling_weekly_rule_shift ON scheduling.weekly_rule USING btree (shift_id);


--
-- Name: uk_scheduling_shift_name_ci; Type: INDEX; Schema: scheduling; Owner: postgres
--

CREATE UNIQUE INDEX uk_scheduling_shift_name_ci ON scheduling.shift USING btree (doctor_clinic_id, lower((name)::text));


--
-- Name: ix_subscription_clinic; Type: INDEX; Schema: subscription; Owner: postgres
--

CREATE INDEX ix_subscription_clinic ON subscription.clinic_subscription USING btree (clinic_id, status, ends_at);


--
-- Name: ix_subscription_status_end; Type: INDEX; Schema: subscription; Owner: postgres
--

CREATE INDEX ix_subscription_status_end ON subscription.clinic_subscription USING btree (status, ends_at);


--
-- Name: uk_subscription_active_clinic; Type: INDEX; Schema: subscription; Owner: postgres
--

CREATE UNIQUE INDEX uk_subscription_active_clinic ON subscription.clinic_subscription USING btree (clinic_id) WHERE ((status)::text = 'ACTIVE'::text);


--
-- Name: clinic fk_clinic_area; Type: FK CONSTRAINT; Schema: clinic; Owner: postgres
--

ALTER TABLE ONLY clinic.clinic
    ADD CONSTRAINT fk_clinic_area FOREIGN KEY (area_id) REFERENCES clinic.area(id) ON DELETE RESTRICT;


--
-- Name: create_idempotency fk_clinic_create_idempotency_clinic; Type: FK CONSTRAINT; Schema: clinic; Owner: postgres
--

ALTER TABLE ONLY clinic.create_idempotency
    ADD CONSTRAINT fk_clinic_create_idempotency_clinic FOREIGN KEY (clinic_id) REFERENCES clinic.clinic(id) ON DELETE CASCADE;


--
-- Name: clinic_phone fk_clinic_phone_clinic; Type: FK CONSTRAINT; Schema: clinic; Owner: postgres
--

ALTER TABLE ONLY clinic.clinic_phone
    ADD CONSTRAINT fk_clinic_phone_clinic FOREIGN KEY (clinic_id) REFERENCES clinic.clinic(id) ON DELETE CASCADE;


--
-- Name: verification_event fk_clinic_verification_event_clinic; Type: FK CONSTRAINT; Schema: clinic; Owner: postgres
--

ALTER TABLE ONLY clinic.verification_event
    ADD CONSTRAINT fk_clinic_verification_event_clinic FOREIGN KEY (clinic_id) REFERENCES clinic.clinic(id) ON DELETE CASCADE;


--
-- Name: doctor_specialty fk_doctor_specialty_doctor; Type: FK CONSTRAINT; Schema: doctor; Owner: postgres
--

ALTER TABLE ONLY doctor.doctor_specialty
    ADD CONSTRAINT fk_doctor_specialty_doctor FOREIGN KEY (doctor_id) REFERENCES doctor.doctor_profile(id) ON DELETE CASCADE;


--
-- Name: doctor_specialty fk_doctor_specialty_specialty; Type: FK CONSTRAINT; Schema: doctor; Owner: postgres
--

ALTER TABLE ONLY doctor.doctor_specialty
    ADD CONSTRAINT fk_doctor_specialty_specialty FOREIGN KEY (specialty_id) REFERENCES doctor.specialty(id) ON DELETE RESTRICT;


--
-- Name: staff_invitation fk_identity_invitation_inviter; Type: FK CONSTRAINT; Schema: identity; Owner: postgres
--

ALTER TABLE ONLY identity.staff_invitation
    ADD CONSTRAINT fk_identity_invitation_inviter FOREIGN KEY (invited_by_user_id) REFERENCES identity.app_user(id) ON DELETE RESTRICT;


--
-- Name: invitation_permission fk_identity_invitation_permission_invitation; Type: FK CONSTRAINT; Schema: identity; Owner: postgres
--

ALTER TABLE ONLY identity.invitation_permission
    ADD CONSTRAINT fk_identity_invitation_permission_invitation FOREIGN KEY (invitation_id) REFERENCES identity.staff_invitation(id) ON DELETE CASCADE;


--
-- Name: membership_permission fk_identity_membership_permission_membership; Type: FK CONSTRAINT; Schema: identity; Owner: postgres
--

ALTER TABLE ONLY identity.membership_permission
    ADD CONSTRAINT fk_identity_membership_permission_membership FOREIGN KEY (membership_id) REFERENCES identity.staff_membership(id) ON DELETE CASCADE;


--
-- Name: staff_membership fk_identity_membership_user; Type: FK CONSTRAINT; Schema: identity; Owner: postgres
--

ALTER TABLE ONLY identity.staff_membership
    ADD CONSTRAINT fk_identity_membership_user FOREIGN KEY (user_id) REFERENCES identity.app_user(id) ON DELETE RESTRICT;


--
-- Name: refresh_session fk_identity_refresh_replacement; Type: FK CONSTRAINT; Schema: identity; Owner: postgres
--

ALTER TABLE ONLY identity.refresh_session
    ADD CONSTRAINT fk_identity_refresh_replacement FOREIGN KEY (replaced_by_session_id) REFERENCES identity.refresh_session(id) ON DELETE SET NULL;


--
-- Name: refresh_session fk_identity_refresh_user; Type: FK CONSTRAINT; Schema: identity; Owner: postgres
--

ALTER TABLE ONLY identity.refresh_session
    ADD CONSTRAINT fk_identity_refresh_user FOREIGN KEY (user_id) REFERENCES identity.app_user(id) ON DELETE CASCADE;


--
-- Name: admin_grant_permission fk_platform_admin_grant_permission; Type: FK CONSTRAINT; Schema: platform_admin; Owner: postgres
--

ALTER TABLE ONLY platform_admin.admin_grant_permission
    ADD CONSTRAINT fk_platform_admin_grant_permission FOREIGN KEY (grant_id) REFERENCES platform_admin.admin_grant(id) ON DELETE CASCADE;


--
-- Name: review_moderation_audit fk_review_moderation_review; Type: FK CONSTRAINT; Schema: review; Owner: postgres
--

ALTER TABLE ONLY review.review_moderation_audit
    ADD CONSTRAINT fk_review_moderation_review FOREIGN KEY (review_id) REFERENCES review.clinic_review(id) ON DELETE CASCADE;


--
-- Name: shift_break fk_scheduling_shift_break_rule; Type: FK CONSTRAINT; Schema: scheduling; Owner: postgres
--

ALTER TABLE ONLY scheduling.shift_break
    ADD CONSTRAINT fk_scheduling_shift_break_rule FOREIGN KEY (rule_id) REFERENCES scheduling.weekly_rule(id) ON DELETE CASCADE;


--
-- Name: weekly_rule fk_scheduling_weekly_rule_shift; Type: FK CONSTRAINT; Schema: scheduling; Owner: postgres
--

ALTER TABLE ONLY scheduling.weekly_rule
    ADD CONSTRAINT fk_scheduling_weekly_rule_shift FOREIGN KEY (shift_id) REFERENCES scheduling.shift(id) ON DELETE CASCADE;


--
-- Name: clinic_subscription fk_subscription_plan; Type: FK CONSTRAINT; Schema: subscription; Owner: postgres
--

ALTER TABLE ONLY subscription.clinic_subscription
    ADD CONSTRAINT fk_subscription_plan FOREIGN KEY (plan_id) REFERENCES subscription.plan(id) ON DELETE RESTRICT;


--
-- PostgreSQL database dump complete
--

\unrestrict hB0lDd6WJ0aByFB5P9aWSHo4OGa1f0aQwwg8lGwEYmHsK5df7EK0mkzj3ZuqAUl

