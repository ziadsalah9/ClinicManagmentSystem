package com.clinic.backend.scheduling.presentation;

import com.clinic.backend.scheduling.domain.ScheduleError;
import com.clinic.backend.scheduling.domain.SchedulingException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.net.URI;

@RestControllerAdvice(basePackages = "com.clinic.backend.scheduling.presentation")
class ScheduleExceptionHandler {
    @ExceptionHandler(SchedulingException.class)
    ProblemDetail handle(SchedulingException exception) {
        HttpStatus status = switch (exception.error()) {
            case INVALID_INPUT, INVALID_DATE_RANGE -> HttpStatus.BAD_REQUEST;
            case ACCESS_DENIED -> HttpStatus.FORBIDDEN;
            case DOCTOR_CLINIC_NOT_FOUND, SHIFT_NOT_FOUND, EXCEPTION_NOT_FOUND -> HttpStatus.NOT_FOUND;
            case STALE_VERSION, SHIFT_NAME_CONFLICT, SHIFT_CONFLICT, BOOKING_CONFLICT -> HttpStatus.CONFLICT;
        };
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(status, exception.getMessage());
        problem.setTitle("Schedule request failed");
        problem.setType(URI.create("https://api.clinic.local/problems/scheduling/"
                + exception.error().name().toLowerCase()));
        problem.setProperty("code", exception.error().name());
        return problem;
    }

    @ExceptionHandler(ObjectOptimisticLockingFailureException.class)
    ProblemDetail concurrentUpdate(ObjectOptimisticLockingFailureException exception) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.CONFLICT,
                "The schedule changed concurrently. Reload and retry.");
        problem.setTitle("Concurrent modification");
        problem.setProperty("code", "CONCURRENT_MODIFICATION");
        return problem;
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    ProblemDetail validation(MethodArgumentNotValidException exception) {
        String detail = exception.getBindingResult().getFieldErrors().stream().findFirst()
                .map(error -> error.getField() + ": " + error.getDefaultMessage())
                .orElse("Request validation failed");
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, detail);
        problem.setTitle("Validation failed");
        problem.setProperty("code", "VALIDATION_FAILED");
        return problem;
    }
}
