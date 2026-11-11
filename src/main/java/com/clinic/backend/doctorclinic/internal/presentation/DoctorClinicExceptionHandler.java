package com.clinic.backend.doctorclinic.internal.presentation;

import com.clinic.backend.doctorclinic.internal.domain.DoctorClinicError;
import com.clinic.backend.doctorclinic.internal.domain.DoctorClinicException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.net.URI;

@RestControllerAdvice(basePackages = "com.clinic.backend.doctorclinic.internal.presentation")
class DoctorClinicExceptionHandler {
    @ExceptionHandler(DoctorClinicException.class)
    ProblemDetail handle(DoctorClinicException exception) {
        HttpStatus status = switch (exception.error()) {
            case INVALID_INPUT -> HttpStatus.BAD_REQUEST;
            case CLINIC_NOT_FOUND, DOCTOR_NOT_FOUND, ASSOCIATION_NOT_FOUND -> HttpStatus.NOT_FOUND;
            case DOCTOR_NOT_MEMBER_OF_CLINIC, STALE_VERSION, RELATIONSHIP_CONFLICT -> HttpStatus.CONFLICT;
            case ACCESS_DENIED -> HttpStatus.FORBIDDEN;
            case CLINIC_NOT_AVAILABLE -> HttpStatus.CONFLICT;
        };
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(status, exception.getMessage());
        problem.setTitle("Doctor clinic request failed");
        problem.setType(URI.create("https://api.clinic.local/problems/doctor-clinic/" + exception.error().name().toLowerCase()));
        problem.setProperty("code", exception.error().name());
        return problem;
    }

    @ExceptionHandler(ObjectOptimisticLockingFailureException.class)
    ProblemDetail concurrentUpdate(ObjectOptimisticLockingFailureException exception) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.CONFLICT,
                "Doctor clinic settings changed concurrently. Reload and retry.");
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
