# Hoppscotch - Clinic Backend (Recommended, no SMS/OTP)

Import:
1. `hoppscotch/clinic-backend-environment.json`
2. `hoppscotch/clinic-backend-collection.json`

Base URL: `http://localhost:8083` (edit `baseUrl` in the environment if needed).

See `SCHEDULING_USAGE_AND_CODE.txt` for the full Scheduling request/response and code guide.

## Refactor note (doctor onboarding separation)
- **Invite** is lightweight: `POST /api/v1/admin/doctors/invite` / `POST /api/v1/owners/doctors/invite` accept only `{phone, fullName, email?, clinicId}`. No professional profile fields at invite time.
- **Claim** creates identity + DOCTOR membership and issues tokens: `POST /api/v1/auth/doctors/claim` with `{token,password,locale?,deviceId?}`.
- **Profile completion** happens after claim: `POST /api/v1/doctors/me` (authenticated doctor token) with full profile (`professionalTitle,bio,yearsExperience,syndicateNumber,gender,specialtyIds`).
- **Direct provision** still accepts full profile and atomically creates identity+membership+profile: `POST /api/v1/admin/doctors/direct`.
- All provisioning/invite endpoints require **mandatory `clinicId`**, validate clinic existence via `ClinicFacade.existsById`, and enforce **OWNER ownership/IDOR** checks.

## Hidden (kept for compatibility/local dev)
- SMS/OTP/provider-style auth: `/api/v1/auth/otp/request`, `/otp/verify`, `/otp/fake*`
- Duplicate password logins: `/api/v1/auth/owner/password-login`, `/doctor/password-login` (use `/password/login`)
- Duplicate doctor provisioning: `/api/v1/admin/doctors`, `/api/v1/owners/doctors` (use `/admin/doctors/direct`, `/admin/doctors/invite`, `/owners/doctors/invite`)

## Recommended flows
### 1) Admin/Owner login
Run **01 - Admin password login** and/or **Owner password login**.

### 2) Catalog + clinic
Run **03 - List areas/specialties** (saves `areaId`, `specialtyId`). Run **04 - Create clinic** / **List my clinics** (saves `clinicId`).

### 3A) Direct provision (full profile now)
Run **02 - Admin/Owner: direct provision doctor**. Temp password is never returned/logged; deliver securely. Doctor logs in, may get `PASSWORD_CHANGE_REQUIRED`, then run **01 - Change password**.

### 3B) Invite + claim + complete profile (lightweight invite)
1. Run **02 - Admin/Owner: invite doctor (LIGHTWEIGHT)** or **Owner: invite doctor for owned clinic**.
2. Deliver claim token via secure channel (LOCAL profile adapter may print for dev only; never log raw tokens in production). Paste into `claimToken`.
3. Run **02 - Doctor: claim onboarding invite** (saves doctor tokens).
4. Run **05 - Doctor: complete/create my profile (after claim)** with valid `specialtyIds`.

### 4) Doctor-clinic settings
Run **05 - Get my doctor profile**, **Upsert doctor-clinic settings**, then **Get all doctors in clinic** (`GET /api/v1/clinics/{clinicId}/doctors`, authenticated clinic member; returns only active doctor-clinic associations). This saves `doctorClinicId`, which the Scheduling requests require.

### 5) Scheduling
Run the **06 - Scheduling** folder with the owner token (or any clinic member with `SCHEDULE_MANAGE`):
1. **List shifts** loads an existing `shiftId`/`shiftVersion`.
2. **Create Sunday shift** creates a calculated-capacity shift. If it already exists, the request reports `409` and you can continue with the listed shift.
3. **Preview capacity** and **Preview shift change** are read-only checks.
4. **Update shift** and **Activate shift** require the current `shiftVersion`; reload on `409 STALE_VERSION`.
5. **List exceptions**, **Create day-off exception**, **Create changed-hours exception**, and **Cancel exception** exercise exception reconciliation.
6. **Generate sessions** and **List sessions** use the 60-day horizon (`scheduleFrom`/`scheduleTo`).

The API base path is `/api/v1/clinics/{clinicId}/doctors/{doctorClinicId}/schedule`. Session generation is idempotent and preserves booked sessions. Updating an active shift returns `409 BOOKING_CONFLICT` if the change would invalidate existing bookings.

### 6) Platform admin clinic verification
1. Log in with **01 - Admin password login**.
2. Run **02 - Admin: list all clinics with owner** or **02 - Admin: filter clinics by status**.
   The filter request uses `clinicStatusFilter` (`PENDING_VERIFICATION`, `ACTIVE`, `SUSPENDED`, `REJECTED`).
   The unfiltered list is an administrator view; only `ACTIVE` clinics are discoverable to patients once public clinic discovery exists.
3. To change state, set `verificationAction` and `verificationReason`, then run **02 - Admin: transition clinic verification**:
   - `APPROVE`: `PENDING_VERIFICATION -> ACTIVE`
   - `REJECT`: `PENDING_VERIFICATION -> REJECTED` (terminal)
   - `REQUEST_INFORMATION`: stays pending
   - `SUSPEND`: `ACTIVE -> SUSPENDED`
   - `REACTIVATE`: `SUSPENDED -> ACTIVE`
   A reason is required for `REJECT`, `REQUEST_INFORMATION` and `SUSPEND`.
4. Run **02 - Admin: get clinic verification history** to view newest-first decisions. Each entry records actor, action, previous/new status, reason and timestamp.
5. `409 STALE_VERSION` means the clinic changed before your decision. Reload the clinic first; do not blindly retry.
6. The same verification controls are available in the web UI at `/admin/clinics` and `/admin/clinics/:clinicId`.

## Environment variables
`baseUrl`, admin/owner/doctor phones+passwords, `adminToken/ownerToken/doctorToken`, `claimToken`, `clinicId/areaId/specialtyId`, `doctorId`, `doctorUserId`, refresh/version vars.
Scheduling extras: `doctorClinicId`, `shiftId`, `shiftVersion`, `exceptionId`, `exceptionVersion`, `scheduleFrom`, `scheduleTo`, `sessionId`, `sessionVersion`.
Verification extras: `clinicStatusFilter`, `verificationAction`, `verificationReason`, `verificationHistoryCount`.

## Operational notes
- Requires Flyway V9 applied for scheduling and Booking-generated sessions. V9 is additive; existing migrations must remain untouched.
- 403 `PASSWORD_CHANGE_REQUIRED`: call `POST /api/v1/auth/password/change`.
- Verification errors: `400 VERIFICATION_REASON_REQUIRED`, `409 INVALID_STATUS_TRANSITION`, `409 STALE_VERSION`.
- Scheduling errors: `400 INVALID_INPUT`/`INVALID_DATE_RANGE`/`VALIDATION_FAILED`/`INVALID_REQUEST_BODY`/`INVALID_PARAMETER`/`MISSING_PARAMETER`, `403 ACCESS_DENIED`, `404 DOCTOR_CLINIC_NOT_FOUND`/`SHIFT_NOT_FOUND`/`EXCEPTION_NOT_FOUND`, `409 STALE_VERSION`/`SHIFT_NAME_CONFLICT`/`SHIFT_CONFLICT`/`BOOKING_CONFLICT`/`CONCURRENT_MODIFICATION`/`SCHEDULE_DATA_CONFLICT`. Responses include `title`, `detail`, `code`, `retryable`, and `errors[]` for validation failures.
