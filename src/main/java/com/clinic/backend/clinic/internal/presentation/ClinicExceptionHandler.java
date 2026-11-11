package com.clinic.backend.clinic.internal.presentation;

import com.clinic.backend.clinic.internal.domain.ClinicError;
import com.clinic.backend.clinic.internal.domain.ClinicException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.net.URI;

@RestControllerAdvice(basePackages = "com.clinic.backend.clinic.internal.presentation")
class ClinicExceptionHandler {
    @ExceptionHandler(ClinicException.class)
    ProblemDetail handle(ClinicException exception) {
        HttpStatus status = switch (exception.error()) {
            case INVALID_INPUT, VERIFICATION_REASON_REQUIRED -> HttpStatus.BAD_REQUEST;
            case CLINIC_NOT_FOUND, AREA_NOT_FOUND -> HttpStatus.NOT_FOUND;
            case ACCESS_DENIED -> HttpStatus.FORBIDDEN;
            case USER_NOT_FOUND -> HttpStatus.UNAUTHORIZED;
            case CLINIC_NAME_ALREADY_EXISTS, STALE_VERSION, IDEMPOTENCY_KEY_REUSED,
                    INVALID_STATUS_TRANSITION -> HttpStatus.CONFLICT;
            case IDEMPOTENCY_IN_PROGRESS -> HttpStatus.CONFLICT;
        };
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(status, exception.getMessage());
        problem.setTitle("Clinic request failed");
        problem.setType(URI.create("https://api.clinic.local/problems/clinic/" + exception.error().name().toLowerCase()));
        problem.setProperty("code", exception.error().name());
        return problem;
    }

    @ExceptionHandler(ObjectOptimisticLockingFailureException.class)
    ProblemDetail handleOptimisticLock(ObjectOptimisticLockingFailureException exception) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.CONFLICT,
                "Clinic data changed concurrently. Reload and retry your update.");
        problem.setTitle("Concurrent modification");
        problem.setProperty("code", "CONCURRENT_MODIFICATION");
        return problem;
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    ProblemDetail handleValidation(MethodArgumentNotValidException exception) {
        String detail = exception.getBindingResult().getFieldErrors().stream().findFirst()
                .map(error -> error.getField() + ": " + error.getDefaultMessage())
                .orElse("Request validation failed");
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, detail);
        problem.setTitle("Validation failed");
        problem.setProperty("code", "VALIDATION_FAILED");
        return problem;
    }
}
