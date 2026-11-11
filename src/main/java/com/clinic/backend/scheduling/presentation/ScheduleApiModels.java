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
            @NotBlank @Size(max = 100) String name,
            @NotNull CapacityMode capacityMode,
            @Min(3) @Max(60) Integer averageVisitMinutes,
            @Min(1) Integer fixedCapacity,
            @NotNull LocalDate effectiveFrom,
            @NotEmpty @Valid List<WeeklyRuleRequest> weeklyRules,
            @PositiveOrZero Long expectedVersion
    ) {
        ScheduleInputs.ShiftInput toInput() {
            return new ScheduleInputs.ShiftInput(name, capacityMode, averageVisitMinutes,
                    fixedCapacity, effectiveFrom, weeklyRules == null ? List.of() : weeklyRules.stream()
                    .map(WeeklyRuleRequest::toInput).toList());
        }
    }

    record WeeklyRuleRequest(
            @NotNull DayOfWeek dayOfWeek,
            @NotNull LocalTime startTime,
            @NotNull LocalTime endTime,
            @Min(0) int followUpQuota,
            @Valid List<BreakRequest> breaks
    ) {
        ScheduleInputs.WeeklyRuleInput toInput() {
            return new ScheduleInputs.WeeklyRuleInput(dayOfWeek, startTime, endTime, followUpQuota,
                    breaks == null ? List.of() : breaks.stream().map(BreakRequest::toInput).toList());
        }
    }

    record BreakRequest(
            @NotNull LocalTime startTime,
            @NotNull LocalTime endTime
    ) {
        ScheduleInputs.BreakInput toInput() {
            return new ScheduleInputs.BreakInput(startTime, endTime);
        }
    }

    record ActivationRequest(
            @NotNull Boolean active,
            @NotNull @PositiveOrZero Long expectedVersion
    ) {
        ScheduleInputs.ActivationInput toInput() {
            return new ScheduleInputs.ActivationInput(active, expectedVersion);
        }
    }

    record ExceptionRequest(
            UUID shiftId,
            @NotNull ScheduleExceptionType type,
            @NotNull LocalDate dateFrom,
            @NotNull LocalDate dateTo,
            LocalTime startTime,
            LocalTime endTime,
            @Size(max = 500) String reason
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
