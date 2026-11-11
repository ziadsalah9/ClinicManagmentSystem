package com.clinic.backend.scheduling.domain.model;

import java.util.Objects;

/**
 * Represents a break period within a shift.
 */
public record ShiftBreak(
        TimeRange timeRange
) {

    public ShiftBreak {
        Objects.requireNonNull(
                timeRange,
                "Break time range is required"
        );
    }
}
