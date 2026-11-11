package com.clinic.backend.doctor.internal.presentation;

import com.clinic.backend.doctor.internal.domain.DoctorError;
import com.clinic.backend.doctor.internal.domain.DoctorException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.net.URI;

@RestControllerAdvice(basePackages = "com.clinic.backend.doctor.internal.presentation")
class DoctorExceptionHandler {
    @ExceptionHandler(DoctorException.class)
    ProblemDetail handle(DoctorException exception) {
        HttpStatus status = switch (exception.error()) {
            case INVALID_INPUT -> HttpStatus.BAD_REQUEST;
            case DOCTOR_PROFILE_NOT_FOUND -> HttpStatus.NOT_FOUND;
            case DOCTOR_PROFILE_ALREADY_EXISTS, STALE_VERSION -> HttpStatus.CONFLICT;
            case NOT_A_DOCTOR, ACCESS_DENIED -> HttpStatus.FORBIDDEN;
            case SPECIALTY_NOT_FOUND -> HttpStatus.BAD_REQUEST;
        };
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(status, exception.getMessage());
        problem.setTitle("Doctor request failed");
        problem.setType(URI.create("https://api.clinic.local/problems/doctor/" + exception.error().name().toLowerCase()));
        problem.setProperty("code", exception.error().name());
        return problem;
    }

    @ExceptionHandler(ObjectOptimisticLockingFailureException.class)
    ProblemDetail concurrentUpdate(ObjectOptimisticLockingFailureException exception) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.CONFLICT,
                "Doctor profile changed concurrently. Reload and retry.");
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
