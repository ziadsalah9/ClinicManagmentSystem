package com.clinic.backend.booking.internal.application;

import com.clinic.backend.booking.BookingSessionInfo;
import com.clinic.backend.booking.BookingSessionSpecification;
import com.clinic.backend.booking.BookingSessionSyncResult;
import com.clinic.backend.booking.internal.infrastructure.persistence.BookingSessionJpaEntity;
import com.clinic.backend.booking.internal.infrastructure.persistence.SpringDataBookingSessionRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

@Service
public class BookingSessionService {
    private final SpringDataBookingSessionRepository sessions;

    public BookingSessionService(SpringDataBookingSessionRepository sessions) {
        this.sessions = sessions;
    }

    @Transactional(readOnly = true)
    public List<BookingSessionInfo> findSessions(UUID doctorClinicId, LocalDate from, LocalDate to) {
        return sessions.findAllByDoctorClinicIdAndSessionDateBetweenOrderBySessionDateAscStartTimeAsc(
                        doctorClinicId, from, to)
                .stream().map(BookingSessionJpaEntity::toInfo).toList();
    }

    @Transactional(readOnly = true)
    public Optional<BookingSessionInfo> findSession(UUID doctorClinicId, UUID shiftId, LocalDate date) {
        return sessions.findByDoctorClinicIdAndShiftIdAndSessionDate(doctorClinicId, shiftId, date)
                .map(BookingSessionJpaEntity::toInfo);
    }

    @Transactional
    public BookingSessionSyncResult synchronize(UUID doctorClinicId, UUID shiftId,
                                                List<BookingSessionSpecification> specifications,
                                                LocalDate horizonStart, LocalDate horizonEnd) {
        Map<LocalDate, BookingSessionSpecification> requested = new LinkedHashMap<>();
        for (BookingSessionSpecification specification : specifications) {
            requested.put(specification.sessionDate(), specification);
        }

        List<BookingSessionJpaEntity> existing = sessions
                .findAllByDoctorClinicIdAndShiftIdOrderBySessionDateAsc(doctorClinicId, shiftId);
        Map<LocalDate, BookingSessionJpaEntity> existingByDate = new LinkedHashMap<>();
        List<UUID> conflictingSessionIds = new ArrayList<>();
        List<BookingSessionJpaEntity> toDelete = new ArrayList<>();

        for (BookingSessionJpaEntity session : existing) {
            BookingSessionSpecification specification = requested.get(session.getSessionDate());
            if (specification == null) {
                if (session.getBookedCount() > 0 || session.getFollowUpBookedCount() > 0) {
                    conflictingSessionIds.add(session.getId());
                } else {
                    toDelete.add(session);
                }
                continue;
            }
            existingByDate.put(session.getSessionDate(), session);
            if (session.getBookedCount() > 0 || session.getFollowUpBookedCount() > 0) {
                if (specification.capacity() < session.getBookedCount()
                        || specification.followUpQuota() < session.getFollowUpBookedCount()
                        || !specification.startTime().equals(session.toInfo().startTime())) {
                    conflictingSessionIds.add(session.getId());
                }
                session.retainBookedSlot(specification);
            } else {
                session.applySpecification(specification);
            }
        }

        for (BookingSessionSpecification specification : requested.values()) {
            if (existingByDate.containsKey(specification.sessionDate())) {
                continue;
            }
            if (specification.sessionDate().isBefore(horizonStart)
                    || specification.sessionDate().isAfter(horizonEnd)) {
                continue;
            }
            existingByDate.put(specification.sessionDate(),
                    BookingSessionJpaEntity.create(doctorClinicId, shiftId, specification));
        }

        if (!toDelete.isEmpty()) {
            sessions.deleteAll(toDelete);
        }
        if (!existingByDate.isEmpty()) {
            sessions.saveAll(existingByDate.values());
        }
        sessions.flush();

        List<BookingSessionInfo> result = sessions
                .findAllByDoctorClinicIdAndShiftIdOrderBySessionDateAsc(doctorClinicId, shiftId)
                .stream().map(BookingSessionJpaEntity::toInfo).toList();
        return new BookingSessionSyncResult(result, List.copyOf(new LinkedHashSet<>(conflictingSessionIds)));
    }
}
