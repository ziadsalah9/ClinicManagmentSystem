package com.clinic.backend.scheduling.application;

import com.clinic.backend.scheduling.domain.model.CapacityMode;
import com.clinic.backend.scheduling.domain.model.ScheduleExceptionType;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.UUID;

public final class ScheduleInputs {
    private ScheduleInputs() { }

    public record ShiftInput(
            String name,
            CapacityMode capacityMode,
            Integer averageVisitMinutes,
            Integer fixedCapacity,
            LocalDate effectiveFrom,
            List<WeeklyRuleInput> weeklyRules
    ) { }

    public record WeeklyRuleInput(
            DayOfWeek dayOfWeek,
            LocalTime startTime,
            LocalTime endTime,
            int followUpQuota,
            List<BreakInput> breaks
    ) { }

    public record BreakInput(LocalTime startTime, LocalTime endTime) { }

    public record ActivationInput(boolean active, Long expectedVersion) { }

    public record ExceptionInput(
            UUID shiftId,
            ScheduleExceptionType type,
            LocalDate dateFrom,
            LocalDate dateTo,
            LocalTime startTime,
            LocalTime endTime,
            String reason
    ) { }

    public record CancelExceptionInput(Long expectedVersion) { }
}
