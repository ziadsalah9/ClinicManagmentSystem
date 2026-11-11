package com.clinic.backend.scheduling.domain.service;

import com.clinic.backend.scheduling.domain.model.CapacityMode;
import com.clinic.backend.scheduling.domain.model.WeeklyShiftRule;

import java.util.Objects;

public final class ScheduleCapacityCalculator {

    private static final int MIN_AVERAGE_VISIT_MINUTES = 3;
    private static final int MAX_AVERAGE_VISIT_MINUTES = 60;
    private static final long MIN_FIXED_VISIT_SECONDS = 180;

    private ScheduleCapacityCalculator() {
        // Utility-style domain service.
    }

    /**
     * Calculates the maximum number of patients
     * that can be accepted by a shift rule.
     */
    public static int calculateCapacity(
            CapacityMode capacityMode,
            Integer averageVisitMinutes,
            Integer fixedCapacity,
            WeeklyShiftRule rule
    ) {

        validateInputs(
                capacityMode,
                averageVisitMinutes,
                fixedCapacity,
                rule
        );

        return switch (capacityMode) {

            case CALCULATED -> {

                long averageVisitSeconds =
                        averageVisitMinutes * 60L;

                long capacity =
                        rule.availableSeconds() / averageVisitSeconds;

                if (capacity <= 0) {
                    throw new IllegalArgumentException(
                            "Shift duration is insufficient for one visit"
                    );
                }

                yield Math.toIntExact(capacity);
            }

            case FIXED -> {

                long estimatedVisitSeconds =
                        rule.availableSeconds() / fixedCapacity;

                validateMinimumVisitDuration(estimatedVisitSeconds);

                yield fixedCapacity;
            }
        };
    }

    /**
     * Calculates the estimated duration of one visit in seconds.
     */
    public static long calculateEstimatedVisitSeconds(
            CapacityMode capacityMode,
            Integer averageVisitMinutes,
            Integer fixedCapacity,
            WeeklyShiftRule rule
    ) {

        validateInputs(
                capacityMode,
                averageVisitMinutes,
                fixedCapacity,
                rule
        );

        return switch (capacityMode) {

            case CALCULATED -> averageVisitMinutes * 60L;

            case FIXED -> {

                long estimatedVisitSeconds =
                        rule.availableSeconds() / fixedCapacity;

                validateMinimumVisitDuration(estimatedVisitSeconds);

                yield estimatedVisitSeconds;
            }
        };
    }

    private static void validateInputs(
            CapacityMode capacityMode,
            Integer averageVisitMinutes,
            Integer fixedCapacity,
            WeeklyShiftRule rule
    ) {

        Objects.requireNonNull(
                capacityMode,
                "Capacity mode is required"
        );

        Objects.requireNonNull(
                rule,
                "Weekly shift rule is required"
        );

        if (capacityMode == CapacityMode.CALCULATED) {

            if (averageVisitMinutes == null
                    || averageVisitMinutes < MIN_AVERAGE_VISIT_MINUTES
                    || averageVisitMinutes > MAX_AVERAGE_VISIT_MINUTES) {

                throw new IllegalArgumentException(
                        "Average visit minutes must be between 3 and 60"
                );
            }

            if (fixedCapacity != null) {
                throw new IllegalArgumentException(
                        "Fixed capacity must be null for CALCULATED mode"
                );
            }

        } else if (capacityMode == CapacityMode.FIXED) {

            if (fixedCapacity == null || fixedCapacity <= 0) {
                throw new IllegalArgumentException(
                        "Fixed capacity must be greater than zero"
                );
            }

            if (averageVisitMinutes != null) {
                throw new IllegalArgumentException(
                        "Average visit minutes must be null for FIXED mode"
                );
            }
        }
    }

    private static void validateMinimumVisitDuration(
            long estimatedVisitSeconds
    ) {

        if (estimatedVisitSeconds < MIN_FIXED_VISIT_SECONDS) {
            throw new IllegalArgumentException(
                    "Fixed capacity results in an average visit duration below 5 minutes"
            );
        }
    }
}
