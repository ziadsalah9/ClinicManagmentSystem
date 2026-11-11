package com.clinic.backend.booking;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.UUID;

/** Public projection of a bookable session owned by Booking. */
public record BookingSessionInfo(
        UUID id,
        UUID doctorClinicId,
        UUID shiftId,
        LocalDate sessionDate,
        LocalTime startTime,
        LocalTime endTime,
        int capacity,
        int bookedCount,
        int followUpQuota,
        int followUpBookedCount,
        String status,
        long version,
        Instant createdAt,
        Instant updatedAt
) {
    public boolean hasBookings() {
        return bookedCount > 0;
    }
}
