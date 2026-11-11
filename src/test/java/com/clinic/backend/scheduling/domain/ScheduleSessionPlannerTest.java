package com.clinic.backend.scheduling.domain;

import com.clinic.backend.scheduling.application.ScheduleSessionPlanner;
import com.clinic.backend.scheduling.domain.model.CapacityMode;
import com.clinic.backend.scheduling.domain.model.ScheduleException;
import com.clinic.backend.scheduling.domain.model.ScheduleExceptionType;
import com.clinic.backend.scheduling.domain.model.ScheduleShift;
import com.clinic.backend.scheduling.domain.model.TimeRange;
import com.clinic.backend.scheduling.domain.model.WeeklyShiftRule;
import org.junit.jupiter.api.Test;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ScheduleSessionPlannerTest {
    private final UUID doctorClinicId = UUID.randomUUID();
    private final LocalDate monday = LocalDate.of(2026, 11, 9);

    private ScheduleShift mondayShift(int followUpQuota) {
        return ScheduleShift.create(doctorClinicId, "Monday clinic", CapacityMode.CALCULATED,
                15, null, monday,
                List.of(new WeeklyShiftRule(DayOfWeek.MONDAY,
                        new TimeRange(LocalTime.of(9, 0), LocalTime.of(10, 0)),
                        followUpQuota, List.of())));
    }

    @Test
    void expandsOnlyMatchingWeekdaysAcrossSixtyDayHorizon() {
        List<ScheduleSessionPlanner.PlannedSession> sessions = ScheduleSessionPlanner.plan(
                mondayShift(2), List.of(), monday, monday.plusDays(59));

        assertEquals(9, sessions.size());
        assertTrue(sessions.stream().allMatch(session -> session.sessionDate().getDayOfWeek() == DayOfWeek.MONDAY));
        assertTrue(sessions.stream().allMatch(session -> session.capacity() == 4));
        assertTrue(sessions.stream().allMatch(session -> session.followUpQuota() == 2));
    }

    @Test
    void dayOffExceptionSuppressesMatchingDateOnly() {
        ScheduleException dayOff = ScheduleException.create(doctorClinicId, null,
                ScheduleExceptionType.DAY_OFF, monday.plusWeeks(1), monday.plusWeeks(1), null, "Leave");

        List<ScheduleSessionPlanner.PlannedSession> sessions = ScheduleSessionPlanner.plan(
                mondayShift(2), List.of(dayOff), monday, monday.plusDays(59));

        assertEquals(8, sessions.size());
        assertTrue(sessions.stream().noneMatch(session -> session.sessionDate().equals(monday.plusWeeks(1))));
    }

    @Test
    void changedHoursRecomputeCapacityAndCapFollowUpQuota() {
        ScheduleException changed = ScheduleException.create(doctorClinicId, null,
                ScheduleExceptionType.CHANGED_HOURS, monday, monday,
                new TimeRange(LocalTime.of(9, 0), LocalTime.of(9, 30)), "Short day");

        ScheduleSessionPlanner.PlannedSession session = ScheduleSessionPlanner.plan(
                mondayShift(3), List.of(changed), monday, monday).getFirst();

        assertEquals(2, session.capacity());
        assertEquals(2, session.followUpQuota());
        assertEquals(LocalTime.of(9, 30), session.endTime());
    }

    @Test
    void inactiveShiftProducesNoSessions() {
        ScheduleShift shift = mondayShift(2).deactivate();

        assertTrue(ScheduleSessionPlanner.plan(shift, List.of(), monday, monday.plusDays(59)).isEmpty());
    }
}
