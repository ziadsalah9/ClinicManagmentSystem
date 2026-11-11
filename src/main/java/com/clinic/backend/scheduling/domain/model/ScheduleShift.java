package com.clinic.backend.scheduling.domain.model;

import com.clinic.backend.scheduling.domain.service.ScheduleCapacityCalculator;

import java.time.LocalDate;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;


public final class ScheduleShift {

    private static final int MIN_AVERAGE_VISIT_MINUTES = 3;
    private static final int MAX_AVERAGE_VISIT_MINUTES = 60;
   private static final long MIN_FIXED_VISIT_SECONDS = 180;

    private final UUID id;
    private final UUID doctorClinicId;

    private final String name;

    private final CapacityMode capacityMode;
    private final Integer averageVisitMinutes;
    private final Integer fixedCapacity;

    private final LocalDate effectiveFrom;

    private final List<WeeklyShiftRule> weeklyRules;

    private final boolean active;

    private ScheduleShift(
            UUID id,
            UUID doctorClinicId,
            String name,
            CapacityMode capacityMode,
            Integer averageVisitMinutes,
            Integer fixedCapacity,
            LocalDate effectiveFrom,
            List<WeeklyShiftRule> weeklyRules,
            boolean active
    ) {

        this.id = Objects.requireNonNull(id, "Shift ID is required");
        this.doctorClinicId = Objects.requireNonNull(
                doctorClinicId,
                "Doctor clinic ID is required"
        );

        this.name = validateName(name);

        this.capacityMode = Objects.requireNonNull(
                capacityMode,
                "Capacity mode is required"
        );

        this.averageVisitMinutes = averageVisitMinutes;
        this.fixedCapacity = fixedCapacity;

        this.effectiveFrom = Objects.requireNonNull(
                effectiveFrom,
                "Effective from date is required"
        );

        this.weeklyRules = List.copyOf(
                Objects.requireNonNull(
                        weeklyRules,
                        "Weekly rules are required"
                )
        );

        this.active = active;

        validateCapacityConfiguration();
        validateWeeklyRules();
    }


    public static ScheduleShift create(
            UUID doctorClinicId,
            String name,
            CapacityMode capacityMode,
            Integer averageVisitMinutes,
            Integer fixedCapacity,
            LocalDate effectiveFrom,
            List<WeeklyShiftRule> weeklyRules
    ) {

        return new ScheduleShift(
                UUID.randomUUID(),
                doctorClinicId,
                name,
                capacityMode,
                averageVisitMinutes,
                fixedCapacity,
                effectiveFrom,
                weeklyRules,
                true
        );
    }

    public static ScheduleShift restore(
            UUID id,
            UUID doctorClinicId,
            String name,
            CapacityMode capacityMode,
            Integer averageVisitMinutes,
            Integer fixedCapacity,
            LocalDate effectiveFrom,
            List<WeeklyShiftRule> weeklyRules,
            boolean active
    ) {
        return new ScheduleShift(id, doctorClinicId, name, capacityMode, averageVisitMinutes,
                fixedCapacity, effectiveFrom, weeklyRules, active);
    }


    private static String validateName(String name) {

        Objects.requireNonNull(name, "Shift name is required");

        String normalizedName = name.trim();

        if (normalizedName.isBlank()) {
            throw new IllegalArgumentException(
                    "Shift name cannot be blank"
            );
        }

        if (normalizedName.length() > 100) {
            throw new IllegalArgumentException(
                    "Shift name cannot exceed 100 characters"
            );
        }

        return normalizedName;
    }

    private void validateCapacityConfiguration() {

        if (capacityMode == CapacityMode.CALCULATED) {

            if (averageVisitMinutes == null) {
                throw new IllegalArgumentException(
                        "Average visit minutes are required for CALCULATED mode"
                );
            }

            if (averageVisitMinutes < MIN_AVERAGE_VISIT_MINUTES
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

    private void validateWeeklyRules() {

        if (weeklyRules.isEmpty()) {
            throw new IllegalArgumentException(
                    "A shift must have at least one weekly rule"
            );
        }

        Set<java.time.DayOfWeek> configuredDays = new HashSet<>();

        for (WeeklyShiftRule rule : weeklyRules) {

            if (!configuredDays.add(rule.dayOfWeek())) {
                throw new IllegalArgumentException(
                        "A shift cannot have multiple rules for the same day"
                );
            }

            int capacity = calculateCapacity(rule);

            if (rule.followUpQuota() > capacity) {
                throw new IllegalArgumentException(
                        "Follow-up quota cannot exceed shift capacity on "
                                + rule.dayOfWeek()
                );
            }
        }
    }

    // --------------------------------------------------
    // Capacity
    // --------------------------------------------------

    /**
     * Calculates the maximum number of patients
     * for a specific weekly rule.
     */

    public int calculateCapacity(WeeklyShiftRule rule) {

        validateRuleBelongsToShift(rule);

        return ScheduleCapacityCalculator.calculateCapacity(
                capacityMode,
                averageVisitMinutes,
                fixedCapacity,
                rule
        );
    }

    public long estimatedVisitSeconds(WeeklyShiftRule rule) {

        validateRuleBelongsToShift(rule);

        return ScheduleCapacityCalculator.calculateEstimatedVisitSeconds(
                capacityMode,
                averageVisitMinutes,
                fixedCapacity,
                rule
        );
    }

    private void validateRuleBelongsToShift(WeeklyShiftRule rule) {

        Objects.requireNonNull(rule, "Weekly rule is required");

        if (!weeklyRules.contains(rule)) {
            throw new IllegalArgumentException(
                    "Weekly rule does not belong to this shift"
            );
        }
    }


    public ScheduleShift updateConfiguration(
            String name,
            CapacityMode capacityMode,
            Integer averageVisitMinutes,
            Integer fixedCapacity,
            LocalDate effectiveFrom,
            List<WeeklyShiftRule> weeklyRules
    ) {

        return new ScheduleShift(
                this.id,
                this.doctorClinicId,
                name,
                capacityMode,
                averageVisitMinutes,
                fixedCapacity,
                effectiveFrom,
                weeklyRules,
                this.active
        );
    }

    public ScheduleShift deactivate() {

        if (!active) {
            return this;
        }

        return new ScheduleShift(
                id,
                doctorClinicId,
                name,
                capacityMode,
                averageVisitMinutes,
                fixedCapacity,
                effectiveFrom,
                weeklyRules,
                false
        );
    }

    public ScheduleShift activate() {

        if (active) {
            return this;
        }

        return new ScheduleShift(
                id,
                doctorClinicId,
                name,
                capacityMode,
                averageVisitMinutes,
                fixedCapacity,
                effectiveFrom,
                weeklyRules,
                true
        );
    }

    // --------------------------------------------------
    // Getters
    // --------------------------------------------------

    public UUID id() {
        return id;
    }

    public UUID doctorClinicId() {
        return doctorClinicId;
    }

    public String name() {
        return name;
    }

    public CapacityMode capacityMode() {
        return capacityMode;
    }

    public Integer averageVisitMinutes() {
        return averageVisitMinutes;
    }

    public Integer fixedCapacity() {
        return fixedCapacity;
    }

    public LocalDate effectiveFrom() {
        return effectiveFrom;
    }

    public List<WeeklyShiftRule> weeklyRules() {
        return weeklyRules;
    }

    public boolean active() {
        return active;
    }
}
