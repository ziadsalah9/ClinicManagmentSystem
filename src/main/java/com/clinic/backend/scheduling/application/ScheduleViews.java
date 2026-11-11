package com.clinic.backend.scheduling.application;

import com.clinic.backend.booking.BookingSessionInfo;
import com.clinic.backend.scheduling.application.ScheduleShiftRepository.StoredShift;
import com.clinic.backend.scheduling.domain.model.ScheduleConflict;
import com.clinic.backend.scheduling.domain.model.ScheduleException;
import com.clinic.backend.scheduling.domain.model.ShiftBreak;
import com.clinic.backend.scheduling.domain.model.TimeRange;
import com.clinic.backend.scheduling.domain.model.WeeklyShiftRule;

import java.time.DayOfWeek;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.UUID;

public final class ScheduleViews {
    private ScheduleViews() { }

    public record ShiftView(
            UUID id,
            UUID doctorClinicId,
            String name,
            String capacityMode,
            Integer averageVisitMinutes,
            Integer fixedCapacity,
            LocalDate effectiveFrom,
            boolean active,
            long version,
            List<WeeklyRuleView> weeklyRules,
            Instant createdAt,
            Instant updatedAt
    ) {
        public static ShiftView from(StoredShift stored) {
            var shift = stored.shift();
            List<WeeklyRuleView> rules = shift.weeklyRules().stream()
                    .map(rule -> WeeklyRuleView.from(shift, rule))
                    .toList();
            return new ShiftView(shift.id(), shift.doctorClinicId(), shift.name(),
                    shift.capacityMode().name(), shift.averageVisitMinutes(), shift.fixedCapacity(),
                    shift.effectiveFrom(), shift.active(), stored.version(), rules,
                    stored.createdAt(), stored.updatedAt());
        }
    }

    public record WeeklyRuleView(
            DayOfWeek dayOfWeek,
            LocalTime startTime,
            LocalTime endTime,
            int followUpQuota,
            List<BreakView> breaks,
            long availableSeconds,
            int capacity,
            long estimatedVisitSeconds
    ) {
        static WeeklyRuleView from(com.clinic.backend.scheduling.domain.model.ScheduleShift shift,
                                   WeeklyShiftRule rule) {
            return new WeeklyRuleView(rule.dayOfWeek(), rule.workingHours().start(),
                    rule.workingHours().end(), rule.followUpQuota(),
                    rule.breaks().stream().map(BreakView::from).toList(),
                    rule.availableSeconds(), shift.calculateCapacity(rule),
                    shift.estimatedVisitSeconds(rule));
        }
    }

    public record BreakView(LocalTime startTime, LocalTime endTime) {
        static BreakView from(ShiftBreak shiftBreak) {
            return new BreakView(shiftBreak.timeRange().start(), shiftBreak.timeRange().end());
        }
    }

    public record ExceptionView(
            UUID id,
            UUID doctorClinicId,
            UUID shiftId,
            String type,
            LocalDate dateFrom,
            LocalDate dateTo,
            LocalTime startTime,
            LocalTime endTime,
            String reason,
            boolean cancelled,
            Instant cancelledAt,
            long version,
            Instant createdAt,
            Instant updatedAt
    ) {
        public static ExceptionView from(ScheduleExceptionRepository.StoredException stored) {
            ScheduleException exception = stored.exception();
            TimeRange changed = exception.changedWorkingHours();
            return new ExceptionView(exception.id(), exception.doctorClinicId(), exception.shiftId(),
                    exception.type().name(), exception.dateFrom(), exception.dateTo(),
                    changed == null ? null : changed.start(), changed == null ? null : changed.end(),
                    exception.reason(), exception.isCancelled(), exception.cancelledAt(),
                    stored.version(), stored.createdAt(), stored.updatedAt());
        }
    }

    public record CapacityPreviewView(
            LocalDate from,
            LocalDate to,
            List<CapacityDayView> sessions
    ) { }

    public record CapacityDayView(
            LocalDate date,
            LocalTime startTime,
            LocalTime endTime,
            int capacity,
            int followUpQuota
    ) { }

    public record ChangePreviewView(
            List<AffectedSessionView> affectedSessions,
            boolean hasBlockingChanges,
            List<ConflictView> conflicts
    ) { }

    public record AffectedSessionView(
            UUID sessionId,
            LocalDate date,
            String change,
            int bookedCount,
            int followUpBookedCount,
            boolean safeToApply,
            String reason
    ) { }

    public record ConflictView(
            UUID firstShiftId,
            UUID secondShiftId,
            DayOfWeek dayOfWeek,
            LocalTime startTime,
            LocalTime endTime
    ) {
        static ConflictView from(ScheduleConflict conflict) {
            return new ConflictView(conflict.firstShiftId(), conflict.secondShiftId(),
                    conflict.dayOfWeek(), conflict.overlappingPeriod().start(),
                    conflict.overlappingPeriod().end());
        }
    }

    public record SessionView(
            UUID id,
            UUID doctorClinicId,
            UUID shiftId,
            LocalDate sessionDate,
            LocalTime startTime,
            LocalTime endTime,
            int capacity,
            int bookedCount,
            int followUpQuota,
            int followUpBookedCount,
            String status,
            long version,
            Instant createdAt,
            Instant updatedAt
    ) {
        public static SessionView from(BookingSessionInfo info) {
            return new SessionView(info.id(), info.doctorClinicId(), info.shiftId(), info.sessionDate(),
                    info.startTime(), info.endTime(), info.capacity(), info.bookedCount(),
                    info.followUpQuota(), info.followUpBookedCount(), info.status(), info.version(),
                    info.createdAt(), info.updatedAt());
        }
    }

    public record SessionGenerationView(
            LocalDate from,
            LocalDate to,
            List<SessionView> sessions,
            List<UUID> conflictingSessionIds
    ) { }
}
