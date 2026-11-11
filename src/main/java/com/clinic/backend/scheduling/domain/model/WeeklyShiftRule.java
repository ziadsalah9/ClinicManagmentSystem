package com.clinic.backend.scheduling.domain.model;

import java.time.DayOfWeek;
import java.util.List;
import java.util.Objects;


public record WeeklyShiftRule(

        DayOfWeek dayOfWeek,

        TimeRange workingHours,

        int followUpQuota,

        List<ShiftBreak> breaks

) {

    public WeeklyShiftRule {

        Objects.requireNonNull(
                dayOfWeek,
                "Day of week is required"
        );

        Objects.requireNonNull(
                workingHours,
                "Working hours are required"
        );

        if (followUpQuota < 0) {
            throw new IllegalArgumentException(
                    "Follow-up quota cannot be negative"
            );
        }

        breaks = List.copyOf(
                Objects.requireNonNull(
                        breaks,
                        "Breaks list is required"
                )
        );

        validateBreaks(workingHours, breaks);
    }

    private static void validateBreaks(
            TimeRange workingHours,
            List<ShiftBreak> breaks
    ) {

        for (int i = 0; i < breaks.size(); i++) {

            TimeRange currentBreak = breaks.get(i).timeRange();

            // Every break must be completely inside working hours.
            if (!workingHours.contains(currentBreak)) {
                throw new IllegalArgumentException(
                        "Break must be inside the shift working hours"
                );
            }

            // Breaks must not overlap with each other.
            for (int j = i + 1; j < breaks.size(); j++) {

                TimeRange nextBreak = breaks.get(j).timeRange();

                if (currentBreak.overlap(nextBreak)) {
                    throw new IllegalArgumentException(
                            "Break periods must not overlap"
                    );
                }
            }
        }

        if (calculateAvailableSeconds(workingHours, breaks) <= 0) {
            throw new IllegalArgumentException(
                    "Shift must have available working time"
            );
        }
    }

    /**
     * Returns the available working time after subtracting breaks.
     * Seconds are used to preserve calculation precision.
     */
    public long availableSeconds() {

        return calculateAvailableSeconds(
                workingHours,
                breaks
        );
    }

    private static long calculateAvailableSeconds(
            TimeRange workingHours,
            List<ShiftBreak> breaks
    ) {

        long totalBreakSeconds = breaks.stream()
                .mapToLong(b -> b.timeRange().durationInSeconds())
                .sum();

        return workingHours.durationInSeconds() - totalBreakSeconds;
    }
}
