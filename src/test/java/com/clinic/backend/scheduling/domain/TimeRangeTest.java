package com.clinic.backend.scheduling.domain;

import com.clinic.backend.scheduling.domain.model.TimeRange;
import org.junit.jupiter.api.Test;

import java.time.LocalTime;

import static org.junit.jupiter.api.Assertions.*;

class TimeRangeTest {

    @Test
    void shouldCalculateDurationInMinutes() {
        TimeRange range = new TimeRange(
                LocalTime.of(9, 0),
                LocalTime.of(14, 0)
        );

        assertEquals(300, range.durationInMinutes());
    }

    @Test
    void shouldCalculateDurationInSeconds() {
        TimeRange range = new TimeRange(
                LocalTime.of(9, 0),
                LocalTime.of(10, 30)
        );

        assertEquals(5400, range.durationInSeconds());
    }

    @Test
    void shouldDetectOverlappingRanges() {
        TimeRange first = new TimeRange(
                LocalTime.of(9, 0),
                LocalTime.of(13, 0)
        );

        TimeRange second = new TimeRange(
                LocalTime.of(12, 0),
                LocalTime.of(16, 0)
        );

        assertTrue(first.overlap(second));
    }

    @Test
    void shouldNotDetectOverlapForAdjacentRanges() {
        TimeRange first = new TimeRange(
                LocalTime.of(9, 0),
                LocalTime.of(13, 0)
        );

        TimeRange second = new TimeRange(
                LocalTime.of(13, 0),
                LocalTime.of(17, 0)
        );

        assertFalse(first.overlap(second));
    }

    @Test
    void shouldDetectWhenRangeContainsAnotherRange() {
        TimeRange shift = new TimeRange(
                LocalTime.of(9, 0),
                LocalTime.of(14, 0)
        );

        TimeRange breakTime = new TimeRange(
                LocalTime.of(11, 0),
                LocalTime.of(11, 30)
        );

        assertTrue(shift.contains(breakTime));
    }

    @Test
    void shouldRejectRangeWhenStartIsAfterEnd() {
        assertThrows(
                IllegalArgumentException.class,
                () -> new TimeRange(
                        LocalTime.of(14, 0),
                        LocalTime.of(9, 0)
                )
        );
    }

    @Test
    void shouldRejectRangeWhenStartEqualsEnd() {
        assertThrows(
                IllegalArgumentException.class,
                () -> new TimeRange(
                        LocalTime.of(9, 0),
                        LocalTime.of(9, 0)
                )
        );
    }
}
