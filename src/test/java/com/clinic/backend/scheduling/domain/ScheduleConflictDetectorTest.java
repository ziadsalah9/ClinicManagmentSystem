package com.clinic.backend.scheduling.domain;



import com.clinic.backend.scheduling.domain.model.*;
import com.clinic.backend.scheduling.domain.service.ScheduleConflictDetector;
import org.junit.jupiter.api.Test;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class ScheduleConflictDetectorTest {

    private final UUID doctorClinicId = UUID.randomUUID();

    private ScheduleShift createShift(
            DayOfWeek day,
            LocalTime start,
            LocalTime end
    ) {

        WeeklyShiftRule rule = new WeeklyShiftRule(
                day,
                new TimeRange(start, end),
                0,
                List.of()
        );

        return ScheduleShift.create(
                doctorClinicId,
                "Test Shift",
                CapacityMode.CALCULATED,
                15,
                null,
                LocalDate.of(2026, 11, 11),
                List.of(rule)
        );
    }

    @Test
    void shouldDetectOverlappingShifts() {

        ScheduleShift morning = createShift(
                DayOfWeek.SUNDAY,
                LocalTime.of(9, 0),
                LocalTime.of(13, 0)
        );

        ScheduleShift evening = createShift(
                DayOfWeek.SUNDAY,
                LocalTime.of(12, 0),
                LocalTime.of(16, 0)
        );

        List<ScheduleConflict> conflicts =
                ScheduleConflictDetector.detectConflicts(
                        evening,
                        List.of(morning)
                );

        assertEquals(1, conflicts.size());

        ScheduleConflict conflict = conflicts.getFirst();

        assertEquals(DayOfWeek.SUNDAY, conflict.dayOfWeek());

        assertEquals(
                LocalTime.of(12, 0),
                conflict.overlappingPeriod().start()
        );

        assertEquals(
                LocalTime.of(13, 0),
                conflict.overlappingPeriod().end()
        );
    }

    @Test
    void shouldNotDetectConflictForAdjacentShifts() {

        ScheduleShift morning = createShift(
                DayOfWeek.SUNDAY,
                LocalTime.of(9, 0),
                LocalTime.of(13, 0)
        );

        ScheduleShift evening = createShift(
                DayOfWeek.SUNDAY,
                LocalTime.of(13, 0),
                LocalTime.of(17, 0)
        );

        List<ScheduleConflict> conflicts =
                ScheduleConflictDetector.detectConflicts(
                        evening,
                        List.of(morning)
                );

        assertTrue(conflicts.isEmpty());
    }

    @Test
    void shouldNotDetectConflictOnDifferentDays() {

        ScheduleShift sundayShift = createShift(
                DayOfWeek.SUNDAY,
                LocalTime.of(9, 0),
                LocalTime.of(14, 0)
        );

        ScheduleShift mondayShift = createShift(
                DayOfWeek.MONDAY,
                LocalTime.of(9, 0),
                LocalTime.of(14, 0)
        );

        List<ScheduleConflict> conflicts =
                ScheduleConflictDetector.detectConflicts(
                        mondayShift,
                        List.of(sundayShift)
                );

        assertTrue(conflicts.isEmpty());
    }

    @Test
    void shouldIgnoreInactiveExistingShift() {

        ScheduleShift morning = createShift(
                DayOfWeek.SUNDAY,
                LocalTime.of(9, 0),
                LocalTime.of(13, 0)
        );

        ScheduleShift evening = createShift(
                DayOfWeek.SUNDAY,
                LocalTime.of(12, 0),
                LocalTime.of(16, 0)
        );

        List<ScheduleConflict> conflicts =
                ScheduleConflictDetector.detectConflicts(
                        evening,
                        List.of(morning.deactivate())
                );

        assertTrue(conflicts.isEmpty());
    }

    @Test
    void shouldReturnEmptyWhenCandidateShiftIsInactive() {

        ScheduleShift morning = createShift(
                DayOfWeek.SUNDAY,
                LocalTime.of(9, 0),
                LocalTime.of(13, 0)
        );

        ScheduleShift evening = createShift(
                DayOfWeek.SUNDAY,
                LocalTime.of(12, 0),
                LocalTime.of(16, 0)
        );

        List<ScheduleConflict> conflicts =
                ScheduleConflictDetector.detectConflicts(
                        evening.deactivate(),
                        List.of(morning)
                );

        assertTrue(conflicts.isEmpty());
    }

    @Test
    void shouldRejectShiftsFromDifferentDoctorClinics() {

        ScheduleShift morning = createShift(
                DayOfWeek.SUNDAY,
                LocalTime.of(9, 0),
                LocalTime.of(13, 0)
        );

        ScheduleShift anotherClinicShift = ScheduleShift.create(
                UUID.randomUUID(),
                "Evening",
                CapacityMode.CALCULATED,
                15,
                null,
                LocalDate.of(2026, 11, 11),
                List.of(
                        new WeeklyShiftRule(
                                DayOfWeek.SUNDAY,
                                new TimeRange(
                                        LocalTime.of(12, 0),
                                        LocalTime.of(16, 0)
                                ),
                                0,
                                List.of()
                        )
                )
        );

        assertThrows(
                IllegalArgumentException.class,
                () -> ScheduleConflictDetector.detectConflicts(
                        anotherClinicShift,
                        List.of(morning)
                )
        );
    }
}
