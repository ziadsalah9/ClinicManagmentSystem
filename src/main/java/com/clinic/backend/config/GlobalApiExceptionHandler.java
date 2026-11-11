package com.clinic.backend.config;

import com.clinic.backend.clinic.internal.domain.ClinicError;
import com.clinic.backend.clinic.internal.domain.ClinicException;
import com.clinic.backend.doctor.internal.domain.DoctorError;
import com.clinic.backend.doctor.internal.domain.DoctorException;
import com.clinic.backend.doctorclinic.internal.domain.DoctorClinicError;
import com.clinic.backend.doctorclinic.internal.domain.DoctorClinicException;
import com.clinic.backend.identity.IdentityFacadeException;
import com.clinic.backend.identity.internal.domain.IdentityError;
import com.clinic.backend.identity.internal.domain.IdentityException;
import com.clinic.backend.provisioning.internal.domain.ProvisioningError;
import com.clinic.backend.provisioning.internal.domain.ProvisioningException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.ConstraintViolationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authorization.AuthorizationDeniedException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingRequestHeaderException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.servlet.resource.NoResourceFoundException;

import java.net.URI;

/**
 * Global fallback API error mapping. Module-specific handlers (scoped by basePackages)
 * take precedence for their controllers; this ensures cross-module/orchestration endpoints
 * and framework errors still return consistent RFC 7807 responses.
 */
@RestControllerAdvice
@Order(Ordered.LOWEST_PRECEDENCE)
public class GlobalApiExceptionHandler {
    private static final Logger log = LoggerFactory.getLogger(GlobalApiExceptionHandler.class);

    @ExceptionHandler(IdentityException.class)
    ProblemDetail handleIdentity(IdentityException ex) {
        HttpStatus status = switch (ex.error()) {
            case INVALID_PHONE, INVALID_INPUT, OTP_INVALID, OTP_EXPIRED, OTP_TOO_MANY_ATTEMPTS,
                    INVALID_VERIFICATION_TOKEN, INVALID_INVITATION, INVALID_PERMISSION, INVALID_ROLE -> HttpStatus.BAD_REQUEST;
            case OTP_RATE_LIMITED, AUTH_RATE_LIMITED -> HttpStatus.TOO_MANY_REQUESTS;
            case USER_ALREADY_EXISTS, ALREADY_MEMBER -> HttpStatus.CONFLICT;
            case USER_NOT_FOUND -> HttpStatus.NOT_FOUND;
            case USER_NOT_ACTIVE, PERMISSION_DENIED, PASSWORD_CHANGE_REQUIRED -> HttpStatus.FORBIDDEN;
            case INVALID_CREDENTIALS, INVALID_REFRESH_TOKEN -> HttpStatus.UNAUTHORIZED;
            case INVITATION_EXPIRED -> HttpStatus.GONE;
        };
        return problem(status, "Identity request failed", ex.getMessage(), ex.error().name(), "identity/" + ex.error().name().toLowerCase());
    }

    @ExceptionHandler(IdentityFacadeException.class)
    ProblemDetail handleIdentityFacade(IdentityFacadeException ex) {
        HttpStatus status = HttpStatus.BAD_REQUEST;
        try {
            IdentityError error = IdentityError.valueOf(ex.code());
            status = switch (error) {
                case INVALID_PHONE, INVALID_INPUT, OTP_INVALID, OTP_EXPIRED, OTP_TOO_MANY_ATTEMPTS,
                        INVALID_VERIFICATION_TOKEN, INVALID_INVITATION, INVALID_PERMISSION, INVALID_ROLE -> HttpStatus.BAD_REQUEST;
                case OTP_RATE_LIMITED, AUTH_RATE_LIMITED -> HttpStatus.TOO_MANY_REQUESTS;
                case USER_ALREADY_EXISTS, ALREADY_MEMBER -> HttpStatus.CONFLICT;
                case USER_NOT_FOUND -> HttpStatus.NOT_FOUND;
                case USER_NOT_ACTIVE, PERMISSION_DENIED, PASSWORD_CHANGE_REQUIRED -> HttpStatus.FORBIDDEN;
                case INVALID_CREDENTIALS, INVALID_REFRESH_TOKEN -> HttpStatus.UNAUTHORIZED;
                case INVITATION_EXPIRED -> HttpStatus.GONE;
            };
        } catch (IllegalArgumentException ignored) { }
        return problem(status, "Identity request failed", ex.getMessage(), ex.code(), "identity/" + ex.code().toLowerCase());
    }

    @ExceptionHandler(ClinicException.class)
    ProblemDetail handleClinic(ClinicException ex) {
        HttpStatus status = switch (ex.error()) {
            case INVALID_INPUT, VERIFICATION_REASON_REQUIRED -> HttpStatus.BAD_REQUEST;
            case CLINIC_NOT_FOUND, AREA_NOT_FOUND -> HttpStatus.NOT_FOUND;
            case ACCESS_DENIED -> HttpStatus.FORBIDDEN;
            case USER_NOT_FOUND -> HttpStatus.UNAUTHORIZED;
            case CLINIC_NAME_ALREADY_EXISTS, STALE_VERSION, IDEMPOTENCY_KEY_REUSED,
                    IDEMPOTENCY_IN_PROGRESS, INVALID_STATUS_TRANSITION -> HttpStatus.CONFLICT;
        };
        return problem(status, "Clinic request failed", ex.getMessage(), ex.error().name(), "clinic/" + ex.error().name().toLowerCase());
    }

    @ExceptionHandler(DoctorException.class)
    ProblemDetail handleDoctor(DoctorException ex) {
        HttpStatus status = switch (ex.error()) {
            case INVALID_INPUT, SPECIALTY_NOT_FOUND -> HttpStatus.BAD_REQUEST;
            case DOCTOR_PROFILE_NOT_FOUND -> HttpStatus.NOT_FOUND;
            case DOCTOR_PROFILE_ALREADY_EXISTS, STALE_VERSION -> HttpStatus.CONFLICT;
            case NOT_A_DOCTOR, ACCESS_DENIED -> HttpStatus.FORBIDDEN;
        };
        return problem(status, "Doctor request failed", ex.getMessage(), ex.error().name(), "doctor/" + ex.error().name().toLowerCase());
    }

    @ExceptionHandler(DoctorClinicException.class)
    ProblemDetail handleDoctorClinic(DoctorClinicException ex) {
        HttpStatus status = switch (ex.error()) {
            case INVALID_INPUT -> HttpStatus.BAD_REQUEST;
            case CLINIC_NOT_FOUND, DOCTOR_NOT_FOUND, ASSOCIATION_NOT_FOUND -> HttpStatus.NOT_FOUND;
            case DOCTOR_NOT_MEMBER_OF_CLINIC, STALE_VERSION, RELATIONSHIP_CONFLICT, CLINIC_NOT_AVAILABLE -> HttpStatus.CONFLICT;
            case ACCESS_DENIED -> HttpStatus.FORBIDDEN;
        };
        return problem(status, "Doctor clinic request failed", ex.getMessage(), ex.error().name(), "doctor-clinic/" + ex.error().name().toLowerCase());
    }

    @ExceptionHandler(ProvisioningException.class)
    ProblemDetail handleProvisioning(ProvisioningException ex) {
        HttpStatus status = switch (ex.error()) {
            case INVALID_INPUT -> HttpStatus.BAD_REQUEST;
            case CLINIC_NOT_FOUND, SPECIALTY_NOT_FOUND -> HttpStatus.NOT_FOUND;
            case DOCTOR_PROFILE_FAILED -> HttpStatus.CONFLICT;
            case ACCESS_DENIED -> HttpStatus.FORBIDDEN;
        };
        return problem(status, "Provisioning request failed", ex.getMessage(), ex.error().name(), "provisioning/" + ex.error().name().toLowerCase());
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    ProblemDetail handleValidation(MethodArgumentNotValidException ex) {
        String detail = ex.getBindingResult().getFieldErrors().stream().findFirst()
                .map(e -> e.getField() + ": " + e.getDefaultMessage())
                .orElse("Request validation failed");
        return problem(HttpStatus.BAD_REQUEST, "Validation failed", detail, "VALIDATION_FAILED", "common/validation_failed");
    }

    @ExceptionHandler(ConstraintViolationException.class)
    ProblemDetail handleConstraint(ConstraintViolationException ex) {
        String detail = ex.getConstraintViolations().stream().findFirst()
                .map(v -> v.getPropertyPath() + ": " + v.getMessage())
                .orElse("Request validation failed");
        return problem(HttpStatus.BAD_REQUEST, "Validation failed", detail, "VALIDATION_FAILED", "common/validation_failed");
    }

    @ExceptionHandler({HttpMessageNotReadableException.class, MethodArgumentTypeMismatchException.class,
            MissingServletRequestParameterException.class, MissingRequestHeaderException.class})
    ProblemDetail handleBadRequest(Exception ex) {
        log.debug("Bad request: {}", ex.getMessage());
        return problem(HttpStatus.BAD_REQUEST, "Bad request", "Malformed or missing request data", "BAD_REQUEST", "common/bad_request");
    }

    @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
    ProblemDetail handleMethodNotAllowed(HttpRequestMethodNotSupportedException ex) {
        return problem(HttpStatus.METHOD_NOT_ALLOWED, "Method not allowed", ex.getMessage(), "METHOD_NOT_ALLOWED", "common/method_not_allowed");
    }

    @ExceptionHandler(NoResourceFoundException.class)
    ProblemDetail handleNotFound(NoResourceFoundException ex) {
        return problem(HttpStatus.NOT_FOUND, "Not found", "Requested resource was not found", "NOT_FOUND", "common/not_found");
    }

    @ExceptionHandler({AccessDeniedException.class, AuthorizationDeniedException.class})
    ProblemDetail handleAccessDenied(Exception ex) {
        return problem(HttpStatus.FORBIDDEN, "Access denied", "You do not have permission to perform this action", "ACCESS_DENIED", "common/access_denied");
    }

    @ExceptionHandler(AuthenticationException.class)
    ProblemDetail handleAuth(AuthenticationException ex) {
        return problem(HttpStatus.UNAUTHORIZED, "Authentication required", "Valid authentication is required", "UNAUTHENTICATED", "common/unauthenticated");
    }

    @ExceptionHandler(ObjectOptimisticLockingFailureException.class)
    ProblemDetail handleOptimisticLock(ObjectOptimisticLockingFailureException ex) {
        return problem(HttpStatus.CONFLICT, "Concurrent modification",
                "The resource changed concurrently. Reload and retry.", "CONCURRENT_MODIFICATION", "common/concurrent_modification");
    }

    @ExceptionHandler(DataIntegrityViolationException.class)
    ProblemDetail handleIntegrity(DataIntegrityViolationException ex, HttpServletRequest request) {
        log.warn("Data integrity violation at {}: {}", request.getRequestURI(), ex.getMostSpecificCause().getMessage());
        return problem(HttpStatus.CONFLICT, "Request conflict",
                "The request conflicts with existing data", "DATA_CONFLICT", "common/data_conflict");
    }

    @ExceptionHandler(Exception.class)
    ProblemDetail handleUnexpected(Exception ex, HttpServletRequest request) {
        log.error("Unhandled exception at {} {}", request.getMethod(), request.getRequestURI(), ex);
        return problem(HttpStatus.INTERNAL_SERVER_ERROR, "Internal server error",
                "An unexpected error occurred. Please try again later.", "INTERNAL_ERROR", "common/internal_error");
    }

    private static ProblemDetail problem(HttpStatus status, String title, String detail, String code, String typePath) {
        ProblemDetail p = ProblemDetail.forStatusAndDetail(status, detail);
        p.setTitle(title);
        p.setType(URI.create("https://api.clinic.local/problems/" + typePath));
        p.setProperty("code", code);
        return p;
    }
}
