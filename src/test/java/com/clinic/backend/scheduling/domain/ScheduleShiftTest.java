package com.clinic.backend.scheduling.domain;

import com.clinic.backend.scheduling.domain.model.CapacityMode;
import com.clinic.backend.scheduling.domain.model.ScheduleShift;
import com.clinic.backend.scheduling.domain.model.TimeRange;
import com.clinic.backend.scheduling.domain.model.WeeklyShiftRule;
import org.junit.jupiter.api.Test;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class ScheduleShiftTest {

    private final UUID doctorClinicId = UUID.randomUUID();

    private WeeklyShiftRule createRule(
            DayOfWeek day,
            LocalTime start,
            LocalTime end,
            int followUpQuota
    ) {
        return new WeeklyShiftRule(
                day,
                new TimeRange(start, end),
                followUpQuota,
                List.of()
        );
    }

    private ScheduleShift createShift(
            String name,
            WeeklyShiftRule rule
    ) {
        return ScheduleShift.create(
                doctorClinicId,
                name,
                CapacityMode.CALCULATED,
                15,
                null,
                LocalDate.of(2026, 11, 11),
                List.of(rule)
        );
    }

    @Test
    void shouldCreateShiftSuccessfully() {

        WeeklyShiftRule sunday = createRule(
                DayOfWeek.SUNDAY,
                LocalTime.of(9, 0),
                LocalTime.of(14, 0),
                3
        );

        ScheduleShift shift = createShift("Morning", sunday);

        assertNotNull(shift.id());
        assertEquals(doctorClinicId, shift.doctorClinicId());
        assertEquals("Morning", shift.name());
        assertTrue(shift.active());
        assertEquals(20, shift.calculateCapacity(sunday));
    }

    @Test
    void shouldRejectFollowUpQuotaGreaterThanCapacity() {

        WeeklyShiftRule sunday = createRule(
                DayOfWeek.SUNDAY,
                LocalTime.of(9, 0),
                LocalTime.of(14, 0),
                21
        );

        assertThrows(
                IllegalArgumentException.class,
                () -> createShift("Morning", sunday)
        );
    }

    @Test
    void shouldRejectDuplicateRulesForSameDay() {

        WeeklyShiftRule firstSundayRule = createRule(
                DayOfWeek.SUNDAY,
                LocalTime.of(9, 0),
                LocalTime.of(13, 0),
                2
        );

        WeeklyShiftRule secondSundayRule = createRule(
                DayOfWeek.SUNDAY,
                LocalTime.of(14, 0),
                LocalTime.of(18, 0),
                2
        );

        assertThrows(
                IllegalArgumentException.class,
                () -> ScheduleShift.create(
                        doctorClinicId,
                        "Morning",
                        CapacityMode.CALCULATED,
                        15,
                        null,
                        LocalDate.of(2026, 11, 11),
                        List.of(
                                firstSundayRule,
                                secondSundayRule
                        )
                )
        );
    }

    @Test
    void shouldRejectEmptyWeeklyRules() {

        assertThrows(
                IllegalArgumentException.class,
                () -> ScheduleShift.create(
                        doctorClinicId,
                        "Morning",
                        CapacityMode.CALCULATED,
                        15,
                        null,
                        LocalDate.of(2026, 11, 11),
                        List.of()
                )
        );
    }

    @Test
    void shouldUpdateConfigurationAndPreserveIdentity() {

        WeeklyShiftRule sunday = createRule(
                DayOfWeek.SUNDAY,
                LocalTime.of(9, 0),
                LocalTime.of(14, 0),
                3
        );

        ScheduleShift original = createShift("Morning", sunday);

        WeeklyShiftRule tuesday = createRule(
                DayOfWeek.TUESDAY,
                LocalTime.of(17, 0),
                LocalTime.of(21, 0),
                2
        );

        ScheduleShift updated = original.updateConfiguration(
                "Evening",
                CapacityMode.CALCULATED,
                10,
                null,
                LocalDate.of(2026, 11, 12),
                List.of(tuesday)
        );

        assertEquals(original.id(), updated.id());
        assertEquals(original.doctorClinicId(), updated.doctorClinicId());

        assertEquals("Morning", original.name());
        assertEquals("Evening", updated.name());

        assertTrue(original.active());
        assertTrue(updated.active());
    }

    @Test
    void shouldDeactivateShiftWithoutChangingOriginalInstance() {

        WeeklyShiftRule sunday = createRule(
                DayOfWeek.SUNDAY,
                LocalTime.of(9, 0),
                LocalTime.of(14, 0),
                3
        );

        ScheduleShift original = createShift("Morning", sunday);

        ScheduleShift deactivated = original.deactivate();

        assertTrue(original.active());
        assertFalse(deactivated.active());
        assertEquals(original.id(), deactivated.id());
    }

    @Test
    void shouldActivateDeactivatedShift() {

        WeeklyShiftRule sunday = createRule(
                DayOfWeek.SUNDAY,
                LocalTime.of(9, 0),
                LocalTime.of(14, 0),
                3
        );

        ScheduleShift original = createShift("Morning", sunday);

        ScheduleShift activated = original
                .deactivate()
                .activate();

        assertTrue(activated.active());
        assertEquals(original.id(), activated.id());
    }

    @Test
    void shouldRejectRuleThatDoesNotBelongToShift() {

        WeeklyShiftRule sunday = createRule(
                DayOfWeek.SUNDAY,
                LocalTime.of(9, 0),
                LocalTime.of(14, 0),
                3
        );

        ScheduleShift shift = createShift("Morning", sunday);

        WeeklyShiftRule foreignRule = createRule(
                DayOfWeek.TUESDAY,
                LocalTime.of(17, 0),
                LocalTime.of(21, 0),
                2
        );

        assertThrows(
                IllegalArgumentException.class,
                () -> shift.calculateCapacity(foreignRule)
        );
    }
}
