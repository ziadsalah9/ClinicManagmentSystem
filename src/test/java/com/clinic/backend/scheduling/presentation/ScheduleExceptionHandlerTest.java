package com.clinic.backend.scheduling.presentation;

import com.clinic.backend.scheduling.domain.ScheduleError;
import com.clinic.backend.scheduling.domain.SchedulingException;
import com.clinic.backend.scheduling.domain.model.ScheduleExceptionType;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.exc.InvalidFormatException;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;

class ScheduleExceptionHandlerTest {
    private final ScheduleExceptionHandler handler = new ScheduleExceptionHandler();

    @Test
    void shiftNotFoundReturnsCustomerFriendly404() {
        ProblemDetail problem = handler.handle(new SchedulingException(
                ScheduleError.SHIFT_NOT_FOUND, "Shift was not found"));

        assertThat(problem.getStatus()).isEqualTo(HttpStatus.NOT_FOUND.value());
        assertThat(problem.getTitle()).isEqualTo("Shift not found");
        assertThat(problem.getDetail()).isEqualTo("Shift was not found");
        assertThat(problem.getProperties())
                .containsEntry("code", "SHIFT_NOT_FOUND")
                .containsEntry("retryable", false);
    }

    @Test
    void bookingConflictReturnsActionable409() {
        ProblemDetail problem = handler.handle(new SchedulingException(
                ScheduleError.BOOKING_CONFLICT,
                "The schedule change would invalidate existing bookings on 2026-11-15. Resolve bookings first."));

        assertThat(problem.getStatus()).isEqualTo(HttpStatus.CONFLICT.value());
        assertThat(problem.getTitle()).isEqualTo("Shift change conflicts with bookings");
        assertThat(problem.getDetail()).contains("invalidate existing bookings", "Resolve bookings first");
        assertThat(problem.getProperties()).containsEntry("code", "BOOKING_CONFLICT");
    }

    @Test
    void invalidDateRangeReturns400WithSpecificTitle() {
        ProblemDetail problem = handler.handle(new SchedulingException(
                ScheduleError.INVALID_DATE_RANGE, "The from date must be on or before the to date"));

        assertThat(problem.getStatus()).isEqualTo(HttpStatus.BAD_REQUEST.value());
        assertThat(problem.getTitle()).isEqualTo("Invalid schedule date range");
        assertThat(problem.getProperties()).containsEntry("code", "INVALID_DATE_RANGE");
    }

    @Test
    void optimisticLockReturnsRetryableConflict() {
        ProblemDetail problem = handler.concurrentUpdate(
                new ObjectOptimisticLockingFailureException(Object.class, "shift-1"));

        assertThat(problem.getStatus()).isEqualTo(HttpStatus.CONFLICT.value());
        assertThat(problem.getDetail()).contains("Reload the latest data and try again");
        assertThat(problem.getProperties())
                .containsEntry("code", "CONCURRENT_MODIFICATION")
                .containsEntry("retryable", true);
    }

    @Test
    void invalidExceptionTypeReturnsAllowedValues() {
        InvalidFormatException cause = new InvalidFormatException(
                null, "Invalid exception type", "TIME_RANGE", ScheduleExceptionType.class);
        cause.prependPath(new JsonMappingException.Reference(null, "type"));
        ProblemDetail problem = handler.unreadableBody(
                new HttpMessageNotReadableException("Invalid body", cause, null));

        assertThat(problem.getStatus()).isEqualTo(HttpStatus.BAD_REQUEST.value());
        assertThat(problem.getDetail())
                .contains("type", "DAY_OFF", "CHANGED_HOURS")
                .contains("Allowed values");
        assertThat(problem.getProperties()).containsEntry("code", "INVALID_REQUEST_BODY");
    }

    @Test
    void invalidDateParameterExplainsIsoFormat() {
        MethodArgumentTypeMismatchException exception = new MethodArgumentTypeMismatchException(
                "11/15/2026", LocalDate.class, "from", null, new IllegalArgumentException("bad date"));
        ProblemDetail problem = handler.typeMismatch(exception);

        assertThat(problem.getStatus()).isEqualTo(HttpStatus.BAD_REQUEST.value());
        assertThat(problem.getDetail()).contains("Invalid date for 'from'", "YYYY-MM-DD");
        assertThat(problem.getProperties()).containsEntry("code", "INVALID_PARAMETER");
    }

    @Test
    void missingDateParameterReturnsClearMessage() {
        ProblemDetail problem = handler.missingParameter(
                new MissingServletRequestParameterException("to", "LocalDate"));

        assertThat(problem.getStatus()).isEqualTo(HttpStatus.BAD_REQUEST.value());
        assertThat(problem.getDetail()).isEqualTo("Missing required request parameter 'to'.");
        assertThat(problem.getProperties()).containsEntry("code", "MISSING_PARAMETER");
    }
}
