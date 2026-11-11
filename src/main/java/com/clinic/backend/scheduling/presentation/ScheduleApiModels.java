package com.clinic.backend.scheduling.presentation;

import com.clinic.backend.scheduling.application.ScheduleInputs;
import com.clinic.backend.scheduling.domain.model.CapacityMode;
import com.clinic.backend.scheduling.domain.model.ScheduleExceptionType;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.UUID;

final class ScheduleApiModels {
    private ScheduleApiModels() { }

    record ShiftRequest(
            @NotBlank(message = "Shift name is required")
            @Size(max = 100, message = "Shift name must be at most 100 characters") String name,
            @NotNull(message = "Capacity mode is required (CALCULATED or FIXED)") CapacityMode capacityMode,
            @Min(value = 3, message = "Average visit minutes must be between 3 and 60")
            @Max(value = 60, message = "Average visit minutes must be between 3 and 60") Integer averageVisitMinutes,
            @Min(value = 1, message = "Fixed capacity must be at least 1") Integer fixedCapacity,
            @NotNull(message = "Effective from date is required") LocalDate effectiveFrom,
            @NotEmpty(message = "At least one weekly rule is required")
            @Valid List<WeeklyRuleRequest> weeklyRules,
            @PositiveOrZero(message = "Expected version must be zero or greater") Long expectedVersion
    ) {
        ScheduleInputs.ShiftInput toInput() {
            return new ScheduleInputs.ShiftInput(name, capacityMode, averageVisitMinutes,
                    fixedCapacity, effectiveFrom, weeklyRules == null ? List.of() : weeklyRules.stream()
                    .map(WeeklyRuleRequest::toInput).toList());
        }
    }

    record WeeklyRuleRequest(
            @NotNull(message = "Day of week is required") DayOfWeek dayOfWeek,
            @NotNull(message = "Weekly rule start time is required") LocalTime startTime,
            @NotNull(message = "Weekly rule end time is required") LocalTime endTime,
            @Min(value = 0, message = "Follow-up quota cannot be negative") int followUpQuota,
            @Valid List<BreakRequest> breaks
    ) {
        ScheduleInputs.WeeklyRuleInput toInput() {
            return new ScheduleInputs.WeeklyRuleInput(dayOfWeek, startTime, endTime, followUpQuota,
                    breaks == null ? List.of() : breaks.stream().map(BreakRequest::toInput).toList());
        }
    }

    record BreakRequest(
            @NotNull(message = "Break start time is required") LocalTime startTime,
            @NotNull(message = "Break end time is required") LocalTime endTime
    ) {
        ScheduleInputs.BreakInput toInput() {
            return new ScheduleInputs.BreakInput(startTime, endTime);
        }
    }

    record ActivationRequest(
            @NotNull(message = "Active flag is required") Boolean active,
            @NotNull(message = "Expected version is required")
            @PositiveOrZero(message = "Expected version must be zero or greater") Long expectedVersion
    ) {
        ScheduleInputs.ActivationInput toInput() {
            return new ScheduleInputs.ActivationInput(active, expectedVersion);
        }
    }

    record ExceptionRequest(
            UUID shiftId,
            @NotNull(message = "Exception type is required (DAY_OFF or CHANGED_HOURS)")
            ScheduleExceptionType type,
            @NotNull(message = "Exception start date is required") LocalDate dateFrom,
            @NotNull(message = "Exception end date is required") LocalDate dateTo,
            LocalTime startTime,
            LocalTime endTime,
            @Size(max = 500, message = "Exception reason must be at most 500 characters") String reason
    ) {
        ScheduleInputs.ExceptionInput toInput() {
            return new ScheduleInputs.ExceptionInput(shiftId, type, dateFrom, dateTo,
                    startTime, endTime, reason);
        }
    }

    record CancelExceptionRequest(@PositiveOrZero Long expectedVersion) {
        ScheduleInputs.CancelExceptionInput toInput() {
            return new ScheduleInputs.CancelExceptionInput(expectedVersion);
        }
    }
}
