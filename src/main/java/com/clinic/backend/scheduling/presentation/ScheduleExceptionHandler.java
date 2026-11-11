package com.clinic.backend.scheduling.presentation;

import com.clinic.backend.scheduling.domain.ScheduleError;
import com.clinic.backend.scheduling.domain.SchedulingException;
import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.exc.InvalidFormatException;
import com.fasterxml.jackson.databind.exc.MismatchedInputException;
import jakarta.validation.ConstraintViolationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.NestedExceptionUtils;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingRequestHeaderException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

import java.net.URI;
import java.time.LocalDate;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

@RestControllerAdvice(basePackages = "com.clinic.backend.scheduling.presentation")
class ScheduleExceptionHandler {
    private static final Logger log = LoggerFactory.getLogger(ScheduleExceptionHandler.class);

    @ExceptionHandler(SchedulingException.class)
    ProblemDetail handle(SchedulingException exception) {
        return problem(exception.error(), exception.getMessage());
    }

    @ExceptionHandler(ObjectOptimisticLockingFailureException.class)
    ProblemDetail concurrentUpdate(ObjectOptimisticLockingFailureException exception) {
        return problem(HttpStatus.CONFLICT,
                "Concurrent schedule change",
                "The schedule changed while you were editing it. Reload the latest data and try again.",
                "CONCURRENT_MODIFICATION", "scheduling/concurrent_modification", true);
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    ProblemDetail validation(MethodArgumentNotValidException exception) {
        List<Map<String, Object>> errors = exception.getBindingResult().getFieldErrors().stream()
                .map(error -> Map.<String, Object>of(
                        "field", error.getField(),
                        "message", message(error.getDefaultMessage(), "Invalid value")))
                .toList();
        String detail = errors.isEmpty()
                ? "The request has invalid values. Check the fields and try again."
                : errors.stream()
                        .map(error -> error.get("field") + ": " + error.get("message"))
                        .collect(Collectors.joining("; "));
        ProblemDetail problem = problem(HttpStatus.BAD_REQUEST, "Schedule validation failed",
                detail, "VALIDATION_FAILED", "scheduling/validation_failed", false);
        problem.setProperty("errors", errors);
        return problem;
    }

    @ExceptionHandler(ConstraintViolationException.class)
    ProblemDetail constraintViolation(ConstraintViolationException exception) {
        List<Map<String, Object>> errors = exception.getConstraintViolations().stream()
                .map(violation -> Map.<String, Object>of(
                        "field", violation.getPropertyPath().toString(),
                        "message", message(violation.getMessage(), "Invalid value")))
                .toList();
        String detail = errors.isEmpty()
                ? "The request has invalid values. Check the fields and try again."
                : errors.stream()
                        .map(error -> error.get("field") + ": " + error.get("message"))
                        .collect(Collectors.joining("; "));
        ProblemDetail problem = problem(HttpStatus.BAD_REQUEST, "Schedule validation failed",
                detail, "VALIDATION_FAILED", "scheduling/validation_failed", false);
        problem.setProperty("errors", errors);
        return problem;
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    ProblemDetail unreadableBody(HttpMessageNotReadableException exception) {
        return problem(HttpStatus.BAD_REQUEST, "Invalid schedule request body",
                unreadableDetail(exception), "INVALID_REQUEST_BODY",
                "scheduling/invalid_request_body", false);
    }

    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    ProblemDetail typeMismatch(MethodArgumentTypeMismatchException exception) {
        return problem(HttpStatus.BAD_REQUEST, "Invalid schedule parameter",
                typeMismatchDetail(exception), "INVALID_PARAMETER",
                "scheduling/invalid_parameter", false);
    }

    @ExceptionHandler(MissingServletRequestParameterException.class)
    ProblemDetail missingParameter(MissingServletRequestParameterException exception) {
        return problem(HttpStatus.BAD_REQUEST, "Missing schedule parameter",
                "Missing required request parameter '" + exception.getParameterName() + "'.",
                "MISSING_PARAMETER", "scheduling/missing_parameter", false);
    }

    @ExceptionHandler(MissingRequestHeaderException.class)
    ProblemDetail missingHeader(MissingRequestHeaderException exception) {
        return problem(HttpStatus.BAD_REQUEST, "Missing request header",
                "Missing required request header '" + exception.getHeaderName() + "'.",
                "MISSING_HEADER", "scheduling/missing_header", false);
    }

    @ExceptionHandler(DataIntegrityViolationException.class)
    ProblemDetail dataIntegrity(DataIntegrityViolationException exception) {
        log.warn("Scheduling data integrity violation: {}",
                NestedExceptionUtils.getMostSpecificCause(exception).getClass().getSimpleName());
        return problem(HttpStatus.CONFLICT, "Schedule data conflict",
                "The schedule conflicts with existing data. Reload the latest schedule and try again.",
                "SCHEDULE_DATA_CONFLICT", "scheduling/data_conflict", true);
    }

    private static ProblemDetail problem(ScheduleError error, String detail) {
        return problem(statusFor(error), titleFor(error), detailOr(error, detail),
                error.name(), "scheduling/" + error.name().toLowerCase(Locale.ROOT),
                error == ScheduleError.STALE_VERSION);
    }

    private static ProblemDetail problem(HttpStatus status, String title, String detail,
                                         String code, String typePath, boolean retryable) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(status, detail);
        problem.setType(URI.create("https://api.clinic.local/problems/" + typePath));
        problem.setTitle(title);
        problem.setProperty("code", code);
        problem.setProperty("retryable", retryable);
        return problem;
    }

    private static HttpStatus statusFor(ScheduleError error) {
        return switch (error) {
            case INVALID_INPUT, INVALID_DATE_RANGE -> HttpStatus.BAD_REQUEST;
            case ACCESS_DENIED -> HttpStatus.FORBIDDEN;
            case DOCTOR_CLINIC_NOT_FOUND, SHIFT_NOT_FOUND, EXCEPTION_NOT_FOUND -> HttpStatus.NOT_FOUND;
            case STALE_VERSION, SHIFT_NAME_CONFLICT, SHIFT_CONFLICT, BOOKING_CONFLICT -> HttpStatus.CONFLICT;
        };
    }

    private static String titleFor(ScheduleError error) {
        return switch (error) {
            case INVALID_INPUT -> "Invalid schedule request";
            case INVALID_DATE_RANGE -> "Invalid schedule date range";
            case ACCESS_DENIED -> "Schedule access denied";
            case DOCTOR_CLINIC_NOT_FOUND -> "Doctor clinic not found";
            case SHIFT_NOT_FOUND -> "Shift not found";
            case EXCEPTION_NOT_FOUND -> "Schedule exception not found";
            case STALE_VERSION -> "Outdated schedule version";
            case SHIFT_NAME_CONFLICT -> "Duplicate shift name";
            case SHIFT_CONFLICT -> "Overlapping shift";
            case BOOKING_CONFLICT -> "Shift change conflicts with bookings";
        };
    }

    private static String defaultDetail(ScheduleError error) {
        return switch (error) {
            case INVALID_INPUT -> "The schedule request is invalid. Check the supplied values and try again.";
            case INVALID_DATE_RANGE -> "The date range is invalid. The start date must be on or before the end date.";
            case ACCESS_DENIED -> "You do not have permission to manage this doctor's schedule.";
            case DOCTOR_CLINIC_NOT_FOUND -> "The doctor clinic could not be found.";
            case SHIFT_NOT_FOUND -> "The requested shift could not be found.";
            case EXCEPTION_NOT_FOUND -> "The requested schedule exception could not be found.";
            case STALE_VERSION -> "The schedule changed. Reload the latest data and try again.";
            case SHIFT_NAME_CONFLICT -> "A shift with this name already exists for this doctor.";
            case SHIFT_CONFLICT -> "This shift overlaps another active shift.";
            case BOOKING_CONFLICT -> "This change would affect existing bookings. Resolve the bookings first.";
        };
    }

    private static String detailOr(ScheduleError error, String detail) {
        return message(detail, defaultDetail(error));
    }

    private static String message(String value, String fallback) {
        return value == null || value.isBlank() ? fallback : value.trim();
    }

    private static String typeMismatchDetail(MethodArgumentTypeMismatchException exception) {
        String name = exception.getName();
        Class<?> requiredType = exception.getRequiredType();
        String value = exception.getValue() == null ? "" : String.valueOf(exception.getValue());
        if (requiredType != null && requiredType.isEnum()) {
            return "Invalid value '" + value + "' for '" + name + "'. Allowed values: "
                    + enumValues(requiredType) + ".";
        }
        if (requiredType == LocalDate.class) {
            return "Invalid date for '" + name + "'. Use ISO format YYYY-MM-DD.";
        }
        if (requiredType == UUID.class) {
            return "Invalid UUID for '" + name + "'.";
        }
        return "Invalid value '" + value + "' for '" + name + "'.";
    }

    private static String unreadableDetail(HttpMessageNotReadableException exception) {
        Throwable cause = NestedExceptionUtils.getMostSpecificCause(exception);
        if (cause instanceof InvalidFormatException invalid) {
            String field = jsonPath(invalid);
            Class<?> targetType = invalid.getTargetType();
            if (targetType != null && targetType.isEnum()) {
                return "Invalid value for '" + field + "'. Allowed values: "
                        + enumValues(targetType) + ".";
            }
            return "Invalid value for '" + field + "'. Check the request field and try again.";
        }
        if (cause instanceof MismatchedInputException missing) {
            String field = jsonPath(missing);
            return field.isBlank()
                    ? "The request body is missing required data."
                    : "Missing or invalid value for '" + field + "'.";
        }
        if (cause instanceof JsonParseException) {
            return "The request body is not valid JSON.";
        }
        String message = exception.getMessage();
        if (message != null && message.contains("Required request body is missing")) {
            return "A request body is required.";
        }
        return "The request body contains malformed or invalid data.";
    }

    private static String jsonPath(JsonMappingException exception) {
        String path = exception.getPath().stream()
                .map(reference -> {
                    if (reference.getFieldName() != null) {
                        return reference.getFieldName();
                    }
                    return reference.getIndex() >= 0 ? "[" + reference.getIndex() + "]" : "?";
                })
                .collect(Collectors.joining("."));
        return path.replace(".[", "[");
    }

    private static String enumValues(Class<?> type) {
        Object[] constants = type.getEnumConstants();
        return constants == null ? "" : Arrays.stream(constants)
                .map(Object::toString)
                .collect(Collectors.joining(", "));
    }
}
