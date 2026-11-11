package com.clinic.backend.booking;

import java.util.List;
import java.util.UUID;

/** Result of reconciling Scheduling-generated sessions with Booking-owned rows. */
public record BookingSessionSyncResult(
        List<BookingSessionInfo> sessions,
        List<UUID> conflictingSessionIds
) { }
