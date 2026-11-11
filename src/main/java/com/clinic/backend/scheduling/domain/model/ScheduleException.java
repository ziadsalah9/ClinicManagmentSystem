package com.clinic.backend.scheduling.domain.model;

import java.time.Instant;
import java.time.LocalDate;
import java.util.Objects;
import java.util.UUID;

/**
 * Represents a temporary schedule exception.
 *
 * If shiftId is null, the exception applies
 * to all shifts belonging to the DoctorClinic.
 *
 * This model does not modify historical sessions
 * or resolve existing bookings.
 */
public record ScheduleException(

        UUID id,

        UUID doctorClinicId,

        UUID shiftId,

        ScheduleExceptionType type,

        LocalDate dateFrom,

        LocalDate dateTo,

        TimeRange changedWorkingHours,

        String reason,

        Instant cancelledAt

) {

    public ScheduleException {

        Objects.requireNonNull(id, "Exception ID is required");

        Objects.requireNonNull(
                doctorClinicId,
                "Doctor clinic ID is required"
        );

        Objects.requireNonNull(
                type,
                "Exception type is required"
        );

        Objects.requireNonNull(
                dateFrom,
                "Start date is required"
        );

        Objects.requireNonNull(
                dateTo,
                "End date is required"
        );

        if (dateFrom.isAfter(dateTo)) {
            throw new IllegalArgumentException(
                    "Start date cannot be after end date"
            );
        }

        reason = normalizeReason(reason);

        validateExceptionConfiguration(
                type,
                changedWorkingHours
        );
    }

    // --------------------------------------------------
    // Factory
    // --------------------------------------------------

    public static ScheduleException create(
            UUID doctorClinicId,
            UUID shiftId,
            ScheduleExceptionType type,
            LocalDate dateFrom,
            LocalDate dateTo,
            TimeRange changedWorkingHours,
            String reason
    ) {

        return new ScheduleException(
                UUID.randomUUID(),
                doctorClinicId,
                shiftId,
                type,
                dateFrom,
                dateTo,
                changedWorkingHours,
                reason,
                null
        );
    }

    public static ScheduleException restore(
            UUID id,
            UUID doctorClinicId,
            UUID shiftId,
            ScheduleExceptionType type,
            LocalDate dateFrom,
            LocalDate dateTo,
            TimeRange changedWorkingHours,
            String reason,
            Instant cancelledAt
    ) {
        return new ScheduleException(id, doctorClinicId, shiftId, type, dateFrom, dateTo,
                changedWorkingHours, reason, cancelledAt);
    }

    public ScheduleException cancel(Instant at) {
        Objects.requireNonNull(at, "Cancellation time is required");
        if (isCancelled()) {
            return this;
        }
        return new ScheduleException(id, doctorClinicId, shiftId, type, dateFrom, dateTo,
                changedWorkingHours, reason, at);
    }

    public boolean isCancelled() {
        return cancelledAt != null;
    }

    // --------------------------------------------------
    // Validation
    // --------------------------------------------------

    private static String normalizeReason(String reason) {

        if (reason == null) {
            return null;
        }

        String normalizedReason = reason.trim();

        if (normalizedReason.isBlank()) {
            return null;
        }

        if (normalizedReason.length() > 500) {
            throw new IllegalArgumentException(
                    "Exception reason cannot exceed 500 characters"
            );
        }

        return normalizedReason;
    }

    private static void validateExceptionConfiguration(
            ScheduleExceptionType type,
            TimeRange changedWorkingHours
    ) {

        switch (type) {

            case DAY_OFF -> {

                if (changedWorkingHours != null) {
                    throw new IllegalArgumentException(
                            "Changed working hours must be null for DAY_OFF"
                    );
                }
            }

            case CHANGED_HOURS -> {

                Objects.requireNonNull(
                        changedWorkingHours,
                        "Changed working hours are required for CHANGED_HOURS"
                );
            }
        }
    }

    // --------------------------------------------------
    // Scope Helpers
    // --------------------------------------------------

    public boolean appliesToAllShifts() {
        return shiftId == null;
    }

    public boolean appliesToShift(UUID targetShiftId) {

        Objects.requireNonNull(
                targetShiftId,
                "Target shift ID is required"
        );

        return shiftId == null || shiftId.equals(targetShiftId);
    }

    public boolean affectsDate(LocalDate date) {

        Objects.requireNonNull(date, "Date is required");

        return !date.isBefore(dateFrom)
                && !date.isAfter(dateTo);
    }

    public boolean isDayOff() {
        return type == ScheduleExceptionType.DAY_OFF;
    }

    public boolean changesWorkingHours() {
        return type == ScheduleExceptionType.CHANGED_HOURS;
    }
}
