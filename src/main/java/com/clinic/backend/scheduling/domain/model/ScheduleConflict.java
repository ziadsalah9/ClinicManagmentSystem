package com.clinic.backend.scheduling.domain.model;

import java.time.DayOfWeek;
import java.util.Objects;
import java.util.UUID;

/**
 * Represents an overlapping period between two shifts
 * on the same day of the week.
 *
 * This is a domain result, not a persisted entity.
 */
public record ScheduleConflict(

        UUID firstShiftId,

        UUID secondShiftId,

        DayOfWeek dayOfWeek,

        TimeRange overlappingPeriod

) {

    public ScheduleConflict {

        Objects.requireNonNull(
                firstShiftId,
                "First shift ID is required"
        );

        Objects.requireNonNull(
                secondShiftId,
                "Second shift ID is required"
        );

        Objects.requireNonNull(
                dayOfWeek,
                "Day of week is required"
        );

        Objects.requireNonNull(
                overlappingPeriod,
                "Overlapping period is required"
        );

        if (firstShiftId.equals(secondShiftId)) {
            throw new IllegalArgumentException(
                    "A shift cannot conflict with itself"
            );
        }
    }
}
