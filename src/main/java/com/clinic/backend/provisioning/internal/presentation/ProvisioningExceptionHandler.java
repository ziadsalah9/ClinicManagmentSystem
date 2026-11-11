package com.clinic.backend.provisioning.internal.presentation;

import com.clinic.backend.provisioning.internal.domain.ProvisioningError;
import com.clinic.backend.provisioning.internal.domain.ProvisioningException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.net.URI;

@RestControllerAdvice(basePackages = "com.clinic.backend.provisioning.internal.presentation")
class ProvisioningExceptionHandler {

    @ExceptionHandler(ProvisioningException.class)
    ProblemDetail handle(ProvisioningException exception) {
        HttpStatus status = switch (exception.error()) {
            case INVALID_INPUT -> HttpStatus.BAD_REQUEST;
            case CLINIC_NOT_FOUND, SPECIALTY_NOT_FOUND -> HttpStatus.NOT_FOUND;
            case DOCTOR_PROFILE_FAILED -> HttpStatus.CONFLICT;
            case ACCESS_DENIED -> HttpStatus.FORBIDDEN;
        };
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(status, exception.getMessage());
        problem.setTitle("Provisioning request failed");
        problem.setType(URI.create("https://api.clinic.local/problems/provisioning/" + exception.error().name().toLowerCase()));
        problem.setProperty("code", exception.error().name());
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
