package com.clinic.backend.booking;

import com.clinic.backend.booking.internal.application.BookingSessionService;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/** Only public entry point for Scheduling to materialize and read Booking-owned sessions. */
@Component
public class BookingFacade {
    private final BookingSessionService sessions;

    public BookingFacade(BookingSessionService sessions) {
        this.sessions = sessions;
    }

    public List<BookingSessionInfo> findSessions(UUID doctorClinicId, LocalDate from, LocalDate to) {
        return sessions.findSessions(doctorClinicId, from, to);
    }

    public Optional<BookingSessionInfo> findSession(UUID doctorClinicId, UUID shiftId, LocalDate date) {
        return sessions.findSession(doctorClinicId, shiftId, date);
    }

    public BookingSessionSyncResult synchronizeSessions(
            UUID doctorClinicId,
            UUID shiftId,
            List<BookingSessionSpecification> specifications,
            LocalDate horizonStart,
            LocalDate horizonEnd
    ) {
        return sessions.synchronize(doctorClinicId, shiftId, specifications, horizonStart, horizonEnd);
    }
}
