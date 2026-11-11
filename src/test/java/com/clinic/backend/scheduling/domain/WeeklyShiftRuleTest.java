package com.clinic.backend.scheduling.domain;

import com.clinic.backend.scheduling.domain.model.ShiftBreak;
import com.clinic.backend.scheduling.domain.model.TimeRange;
import com.clinic.backend.scheduling.domain.model.WeeklyShiftRule;
import org.junit.jupiter.api.Test;

import java.time.DayOfWeek;
import java.time.LocalTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class WeeklyShiftRuleTest {

    @Test
    void shouldCalculateAvailableTimeAfterBreaks() {

        WeeklyShiftRule rule = new WeeklyShiftRule(
                DayOfWeek.SUNDAY,
                new TimeRange(
                        LocalTime.of(9, 0),
                        LocalTime.of(14, 0)
                ),
                3,
                List.of(
                        new ShiftBreak(
                                new TimeRange(
                                        LocalTime.of(11, 0),
                                        LocalTime.of(11, 15)
                                )
                        ),
                        new ShiftBreak(
                                new TimeRange(
                                        LocalTime.of(12, 30),
                                        LocalTime.of(12, 45)
                                )
                        )
                )
        );

        assertEquals(16200, rule.availableSeconds());
    }

    @Test
    void shouldRejectBreakOutsideWorkingHours() {

        assertThrows(
                IllegalArgumentException.class,
                () -> new WeeklyShiftRule(
                        DayOfWeek.SUNDAY,
                        new TimeRange(
                                LocalTime.of(9, 0),
                                LocalTime.of(14, 0)
                        ),
                        2,
                        List.of(
                                new ShiftBreak(
                                        new TimeRange(
                                                LocalTime.of(14, 0),
                                                LocalTime.of(14, 30)
                                        )
                                )
                        )
                )
        );
    }

    @Test
    void shouldRejectOverlappingBreaks() {

        assertThrows(
                IllegalArgumentException.class,
                () -> new WeeklyShiftRule(
                        DayOfWeek.SUNDAY,
                        new TimeRange(
                                LocalTime.of(9, 0),
                                LocalTime.of(14, 0)
                        ),
                        2,
                        List.of(
                                new ShiftBreak(
                                        new TimeRange(
                                                LocalTime.of(11, 0),
                                                LocalTime.of(11, 30)
                                        )
                                ),
                                new ShiftBreak(
                                        new TimeRange(
                                                LocalTime.of(11, 15),
                                                LocalTime.of(11, 45)
                                        )
                                )
                        )
                )
        );
    }

    @Test
    void shouldAllowAdjacentBreaks() {

        WeeklyShiftRule rule = new WeeklyShiftRule(
                DayOfWeek.SUNDAY,
                new TimeRange(
                        LocalTime.of(9, 0),
                        LocalTime.of(14, 0)
                ),
                2,
                List.of(
                        new ShiftBreak(
                                new TimeRange(
                                        LocalTime.of(11, 0),
                                        LocalTime.of(11, 15)
                                )
                        ),
                        new ShiftBreak(
                                new TimeRange(
                                        LocalTime.of(11, 15),
                                        LocalTime.of(11, 30)
                                )
                        )
                )
        );

        assertEquals(270 * 60, rule.availableSeconds());
    }

    @Test
    void shouldRejectNegativeFollowUpQuota() {

        assertThrows(
                IllegalArgumentException.class,
                () -> new WeeklyShiftRule(
                        DayOfWeek.SUNDAY,
                        new TimeRange(
                                LocalTime.of(9, 0),
                                LocalTime.of(14, 0)
                        ),
                        -1,
                        List.of()
                )
        );
    }

    @Test
    void shouldRejectShiftWithNoAvailableWorkingTime() {

        assertThrows(
                IllegalArgumentException.class,
                () -> new WeeklyShiftRule(
                        DayOfWeek.SUNDAY,
                        new TimeRange(
                                LocalTime.of(9, 0),
                                LocalTime.of(10, 0)
                        ),
                        0,
                        List.of(
                                new ShiftBreak(
                                        new TimeRange(
                                                LocalTime.of(9, 0),
                                                LocalTime.of(10, 0)
                                        )
                                )
                        )
                )
        );
    }
}
