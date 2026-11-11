package com.clinic.backend.scheduling.domain;

import com.clinic.backend.scheduling.domain.model.ScheduleException;
import com.clinic.backend.scheduling.domain.model.ScheduleExceptionType;
import com.clinic.backend.scheduling.domain.model.TimeRange;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class ScheduleExceptionTest {

    private final UUID doctorClinicId = UUID.randomUUID();
    private final UUID shiftId = UUID.randomUUID();

    @Test
    void shouldCreateDayOffForAllShifts() {

        ScheduleException exception = ScheduleException.create(
                doctorClinicId,
                null,
                ScheduleExceptionType.DAY_OFF,
                LocalDate.of(2026, 11, 15),
                LocalDate.of(2026, 11, 17),
                null,
                "Personal leave"
        );

        assertTrue(exception.isDayOff());
        assertTrue(exception.appliesToAllShifts());
        assertNull(exception.changedWorkingHours());
    }

    @Test
    void shouldCreateDayOffForSpecificShift() {

        ScheduleException exception = ScheduleException.create(
                doctorClinicId,
                shiftId,
                ScheduleExceptionType.DAY_OFF,
                LocalDate.of(2026, 11, 15),
                LocalDate.of(2026, 11, 15),
                null,
                "Doctor unavailable"
        );

        assertFalse(exception.appliesToAllShifts());
        assertTrue(exception.appliesToShift(shiftId));
        assertFalse(exception.appliesToShift(UUID.randomUUID()));
    }

    @Test
    void shouldCreateChangedHoursException() {

        TimeRange changedHours = new TimeRange(
                LocalTime.of(18, 0),
                LocalTime.of(21, 0)
        );

        ScheduleException exception = ScheduleException.create(
                doctorClinicId,
                shiftId,
                ScheduleExceptionType.CHANGED_HOURS,
                LocalDate.of(2026, 11, 17),
                LocalDate.of(2026, 11, 17),
                changedHours,
                "Temporary change"
        );

        assertTrue(exception.changesWorkingHours());
        assertEquals(changedHours, exception.changedWorkingHours());
    }

    @Test
    void shouldRejectChangedHoursWithoutNewWorkingHours() {

        assertThrows(
                NullPointerException.class,
                () -> ScheduleException.create(
                        doctorClinicId,
                        shiftId,
                        ScheduleExceptionType.CHANGED_HOURS,
                        LocalDate.of(2026, 11, 17),
                        LocalDate.of(2026, 11, 17),
                        null,
                        "Temporary change"
                )
        );
    }

    @Test
    void shouldRejectDayOffWithChangedWorkingHours() {

        TimeRange changedHours = new TimeRange(
                LocalTime.of(18, 0),
                LocalTime.of(21, 0)
        );

        assertThrows(
                IllegalArgumentException.class,
                () -> ScheduleException.create(
                        doctorClinicId,
                        shiftId,
                        ScheduleExceptionType.DAY_OFF,
                        LocalDate.of(2026, 11, 17),
                        LocalDate.of(2026, 11, 17),
                        changedHours,
                        "Invalid configuration"
                )
        );
    }

    @Test
    void shouldRejectEndDateBeforeStartDate() {

        assertThrows(
                IllegalArgumentException.class,
                () -> ScheduleException.create(
                        doctorClinicId,
                        shiftId,
                        ScheduleExceptionType.DAY_OFF,
                        LocalDate.of(2026, 11, 20),
                        LocalDate.of(2026, 11, 15),
                        null,
                        "Invalid dates"
                )
        );
    }

    @Test
    void shouldIncludeBothStartAndEndDates() {

        ScheduleException exception = ScheduleException.create(
                doctorClinicId,
                shiftId,
                ScheduleExceptionType.DAY_OFF,
                LocalDate.of(2026, 11, 15),
                LocalDate.of(2026, 11, 17),
                null,
                null
        );

        assertTrue(exception.affectsDate(LocalDate.of(2026, 11, 15)));
        assertTrue(exception.affectsDate(LocalDate.of(2026, 11, 16)));
        assertTrue(exception.affectsDate(LocalDate.of(2026, 11, 17)));

        assertFalse(exception.affectsDate(LocalDate.of(2026, 11, 14)));
        assertFalse(exception.affectsDate(LocalDate.of(2026, 11, 18)));
    }

    @Test
    void shouldNormalizeReason() {

        ScheduleException exception = ScheduleException.create(
                doctorClinicId,
                shiftId,
                ScheduleExceptionType.DAY_OFF,
                LocalDate.of(2026, 11, 15),
                LocalDate.of(2026, 11, 15),
                null,
                "  Personal leave  "
        );

        assertEquals("Personal leave", exception.reason());
    }

    @Test
    void shouldRejectReasonLongerThan500Characters() {

        String longReason = "A".repeat(501);

        assertThrows(
                IllegalArgumentException.class,
                () -> ScheduleException.create(
                        doctorClinicId,
                        shiftId,
                        ScheduleExceptionType.DAY_OFF,
                        LocalDate.of(2026, 11, 15),
                        LocalDate.of(2026, 11, 15),
                        null,
                        longReason
                )
        );
    }
}
