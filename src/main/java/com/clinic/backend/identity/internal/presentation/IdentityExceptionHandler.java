package com.clinic.backend.identity.internal.presentation;

import com.clinic.backend.identity.internal.domain.IdentityError;
import com.clinic.backend.identity.internal.domain.IdentityException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.net.URI;

@RestControllerAdvice(basePackages = "com.clinic.backend.identity.internal.presentation")
public class IdentityExceptionHandler {

    @ExceptionHandler(IdentityException.class)
    ProblemDetail handleIdentity(IdentityException exception) {
        HttpStatus status = statusFor(exception.error());
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(status, exception.getMessage());
        problem.setTitle("Identity request failed");
        problem.setType(URI.create("https://api.clinic.local/problems/identity/" + exception.error().name().toLowerCase()));
        problem.setProperty("code", exception.error().name());
        return problem;
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    ProblemDetail handleValidation(MethodArgumentNotValidException exception) {
        String detail = exception.getBindingResult().getFieldErrors().stream()
                .findFirst()
                .map(error -> error.getField() + ": " + error.getDefaultMessage())
                .orElse("Request validation failed");
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, detail);
        problem.setTitle("Validation failed");
        problem.setProperty("code", "VALIDATION_FAILED");
        return problem;
    }

    private HttpStatus statusFor(IdentityError error) {
        return switch (error) {
            case INVALID_PHONE, INVALID_INPUT, OTP_INVALID, OTP_EXPIRED, OTP_TOO_MANY_ATTEMPTS,
                    INVALID_VERIFICATION_TOKEN, INVALID_INVITATION, INVALID_PERMISSION, INVALID_ROLE -> HttpStatus.BAD_REQUEST;
            case OTP_RATE_LIMITED, AUTH_RATE_LIMITED -> HttpStatus.TOO_MANY_REQUESTS;
            case USER_ALREADY_EXISTS, ALREADY_MEMBER -> HttpStatus.CONFLICT;
            case USER_NOT_FOUND -> HttpStatus.NOT_FOUND;
            case USER_NOT_ACTIVE, PERMISSION_DENIED, PASSWORD_CHANGE_REQUIRED -> HttpStatus.FORBIDDEN;
            case INVALID_CREDENTIALS, INVALID_REFRESH_TOKEN -> HttpStatus.UNAUTHORIZED;
            case INVITATION_EXPIRED -> HttpStatus.GONE;
        };
    }
}
