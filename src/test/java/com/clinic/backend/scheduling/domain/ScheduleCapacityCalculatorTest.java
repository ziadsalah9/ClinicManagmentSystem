package com.clinic.backend.scheduling.domain;


import com.clinic.backend.scheduling.domain.model.CapacityMode;
import com.clinic.backend.scheduling.domain.model.TimeRange;
import com.clinic.backend.scheduling.domain.model.WeeklyShiftRule;
import com.clinic.backend.scheduling.domain.service.ScheduleCapacityCalculator;
import org.junit.jupiter.api.Test;

import java.time.DayOfWeek;
import java.time.LocalTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class ScheduleCapacityCalculatorTest {

    private WeeklyShiftRule createRule(int availableMinutes) {

        return new WeeklyShiftRule(
                DayOfWeek.SUNDAY,
                new TimeRange(
                        LocalTime.of(9, 0),
                        LocalTime.of(9, 0).plusMinutes(availableMinutes)
                ),
                0,
                List.of()
        );
    }

    @Test
    void shouldCalculateCapacityInCalculatedMode() {

        WeeklyShiftRule rule = createRule(270);

        int capacity =
                ScheduleCapacityCalculator.calculateCapacity(
                        CapacityMode.CALCULATED,
                        15,
                        null,
                        rule
                );

        assertEquals(18, capacity);
    }

    @Test
    void shouldCalculateEstimatedVisitSecondsInCalculatedMode() {

        WeeklyShiftRule rule = createRule(270);

        long estimatedSeconds =
                ScheduleCapacityCalculator.calculateEstimatedVisitSeconds(
                        CapacityMode.CALCULATED,
                        15,
                        null,
                        rule
                );

        assertEquals(900, estimatedSeconds);
    }

    @Test
    void shouldCalculateCapacityInFixedMode() {

        WeeklyShiftRule rule = createRule(240);

        int capacity =
                ScheduleCapacityCalculator.calculateCapacity(
                        CapacityMode.FIXED,
                        null,
                        16,
                        rule
                );

        assertEquals(16, capacity);
    }

    @Test
    void shouldCalculateEstimatedVisitSecondsInFixedMode() {

        WeeklyShiftRule rule = createRule(240);

        long estimatedSeconds =
                ScheduleCapacityCalculator.calculateEstimatedVisitSeconds(
                        CapacityMode.FIXED,
                        null,
                        16,
                        rule
                );

        assertEquals(900, estimatedSeconds);
    }

    @Test
    void shouldAllowExactlyThreeMinutesInFixedMode() {

        WeeklyShiftRule rule = createRule(240);

        int capacity =
                ScheduleCapacityCalculator.calculateCapacity(
                        CapacityMode.FIXED,
                        null,
                        80,
                        rule
                );

        assertEquals(80, capacity);
    }

    @Test
    void shouldRejectFixedCapacityBelowThreeMinutesPerVisit() {

        WeeklyShiftRule rule = createRule(240);

        assertThrows(
                IllegalArgumentException.class,
                () -> ScheduleCapacityCalculator.calculateCapacity(
                        CapacityMode.FIXED,
                        null,
                        81,
                        rule
                )
        );
    }

    @Test
    void shouldRejectAverageVisitMinutesBelowMinimum() {

        WeeklyShiftRule rule = createRule(270);

        assertThrows(
                IllegalArgumentException.class,
                () -> ScheduleCapacityCalculator.calculateCapacity(
                        CapacityMode.CALCULATED,
                        2,
                        null,
                        rule
                )
        );
    }

    @Test
    void shouldRejectFixedCapacityInCalculatedMode() {

        WeeklyShiftRule rule = createRule(270);

        assertThrows(
                IllegalArgumentException.class,
                () -> ScheduleCapacityCalculator.calculateCapacity(
                        CapacityMode.CALCULATED,
                        15,
                        10,
                        rule
                )
        );
    }

    @Test
    void shouldRejectAverageVisitMinutesInFixedMode() {

        WeeklyShiftRule rule = createRule(240);

        assertThrows(
                IllegalArgumentException.class,
                () -> ScheduleCapacityCalculator.calculateCapacity(
                        CapacityMode.FIXED,
                        15,
                        16,
                        rule
                )
        );
    }

    @Test
    void shouldRejectZeroFixedCapacity() {

        WeeklyShiftRule rule = createRule(240);

        assertThrows(
                IllegalArgumentException.class,
                () -> ScheduleCapacityCalculator.calculateCapacity(
                        CapacityMode.FIXED,
                        null,
                        0,
                        rule
                )
        );
    }
}
