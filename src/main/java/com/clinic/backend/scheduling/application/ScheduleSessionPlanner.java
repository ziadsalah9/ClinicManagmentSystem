package com.clinic.backend.scheduling.application;

import com.clinic.backend.scheduling.domain.model.CapacityMode;
import com.clinic.backend.scheduling.domain.model.ScheduleException;
import com.clinic.backend.scheduling.domain.model.ScheduleShift;
import com.clinic.backend.scheduling.domain.model.TimeRange;
import com.clinic.backend.scheduling.domain.model.WeeklyShiftRule;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/** Pure schedule-to-session expansion used by previews and materialization. */
public final class ScheduleSessionPlanner {
    private static final long MIN_FIXED_VISIT_SECONDS = 180;

    private ScheduleSessionPlanner() { }

    public static List<PlannedSession> plan(ScheduleShift shift,
                                            List<ScheduleException> exceptions,
                                            LocalDate horizonStart,
                                            LocalDate horizonEnd) {
        if (!shift.active()) {
            return List.of();
        }
        List<ScheduleException> activeExceptions = exceptions.stream()
                .filter(exception -> !exception.isCancelled())
                .toList();
        List<PlannedSession> sessions = new ArrayList<>();
        for (LocalDate cursor = horizonStart; !cursor.isAfter(horizonEnd); cursor = cursor.plusDays(1)) {
            LocalDate date = cursor;
            if (date.isBefore(shift.effectiveFrom())) {
                continue;
            }
            WeeklyShiftRule rule = shift.weeklyRules().stream()
                    .filter(candidate -> candidate.dayOfWeek() == date.getDayOfWeek())
                    .findFirst()
                    .orElse(null);
            if (rule == null) {
                continue;
            }
            List<ScheduleException> applicable = applicableExceptions(activeExceptions, shift, date);
            if (applicable.stream().anyMatch(ScheduleException::isDayOff)) {
                continue;
            }
            ScheduleException changed = applicable.stream()
                    .filter(ScheduleException::changesWorkingHours)
                    .findFirst()
                    .orElse(null);
            TimeRange hours = changed == null ? rule.workingHours() : changed.changedWorkingHours();
            PlannedSession planned = changed == null
                    ? normalSession(shift, rule, date, hours)
                    : changedSession(shift, rule, date, hours);
            if (planned != null) {
                sessions.add(planned);
            }
        }
        return List.copyOf(sessions);
    }

    private static List<ScheduleException> applicableExceptions(List<ScheduleException> exceptions,
                                                                 ScheduleShift shift,
                                                                 LocalDate date) {
        List<ScheduleException> applicable = exceptions.stream()
                .filter(exception -> exception.affectsDate(date))
                .filter(exception -> exception.appliesToShift(shift.id()))
                .toList();
        return applicable.stream()
                .sorted(Comparator.comparing((ScheduleException exception) -> exception.shiftId() == null))
                .toList();
    }

    private static PlannedSession normalSession(ScheduleShift shift, WeeklyShiftRule rule,
                                                LocalDate date, TimeRange hours) {
        int capacity = shift.calculateCapacity(rule);
        return new PlannedSession(date, hours.start(), hours.end(), capacity, rule.followUpQuota());
    }

    private static PlannedSession changedSession(ScheduleShift shift, WeeklyShiftRule rule,
                                                 LocalDate date, TimeRange hours) {
        long availableSeconds = hours.durationInSeconds();
        int capacity;
        if (shift.capacityMode() == CapacityMode.CALCULATED) {
            capacity = Math.toIntExact(availableSeconds / (shift.averageVisitMinutes() * 60L));
        } else {
            capacity = Math.min(shift.fixedCapacity(), Math.toIntExact(availableSeconds / MIN_FIXED_VISIT_SECONDS));
        }
        if (capacity <= 0) {
            return null;
        }
        return new PlannedSession(date, hours.start(), hours.end(), capacity,
                Math.min(rule.followUpQuota(), capacity));
    }

    public record PlannedSession(
            LocalDate sessionDate,
            LocalTime startTime,
            LocalTime endTime,
            int capacity,
            int followUpQuota
    ) { }
}
