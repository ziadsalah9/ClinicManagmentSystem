package com.clinic.backend.scheduling.application;

import com.clinic.backend.booking.BookingFacade;
import com.clinic.backend.booking.BookingSessionInfo;
import com.clinic.backend.booking.BookingSessionSpecification;
import com.clinic.backend.booking.BookingSessionSyncResult;
import com.clinic.backend.doctorclinic.DoctorClinicFacade;
import com.clinic.backend.doctorclinic.DoctorClinicScheduleInfo;
import com.clinic.backend.identity.IdentityFacade;
import com.clinic.backend.scheduling.application.ScheduleExceptionRepository.StoredException;
import com.clinic.backend.scheduling.application.ScheduleShiftRepository.StoredShift;
import com.clinic.backend.scheduling.domain.ScheduleError;
import com.clinic.backend.scheduling.domain.SchedulingException;
import com.clinic.backend.scheduling.domain.model.ScheduleException;
import com.clinic.backend.scheduling.domain.model.ScheduleShift;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

@Service
public class ScheduleHorizonService {
    public static final int HORIZON_DAYS = 60;

    private final ScheduleShiftRepository shifts;
    private final ScheduleExceptionRepository exceptions;
    private final ScheduleGuardRepository guards;
    private final DoctorClinicFacade doctorClinics;
    private final IdentityFacade identity;
    private final BookingFacade bookings;
    private final Clock clock;

    public ScheduleHorizonService(ScheduleShiftRepository shifts,
                                  ScheduleExceptionRepository exceptions,
                                  ScheduleGuardRepository guards,
                                  DoctorClinicFacade doctorClinics,
                                  IdentityFacade identity,
                                  BookingFacade bookings,
                                  Clock clock) {
        this.shifts = shifts;
        this.exceptions = exceptions;
        this.guards = guards;
        this.doctorClinics = doctorClinics;
        this.identity = identity;
        this.bookings = bookings;
        this.clock = clock;
    }

    @Transactional
    public ScheduleViews.SessionGenerationView generate(UUID doctorClinicId) {
        DoctorClinicScheduleInfo info = doctorClinics.findScheduleInfo(doctorClinicId)
                .orElseThrow(() -> new SchedulingException(ScheduleError.DOCTOR_CLINIC_NOT_FOUND,
                        "Doctor-clinic association was not found"));
        return reconcile(info);
    }

    /** Must run inside an existing transaction. */
    public ScheduleViews.SessionGenerationView reconcile(DoctorClinicScheduleInfo info) {
        guards.lockDoctorClinic(info.id());
        LocalDate from = LocalDate.now(clock.withZone(zone(info)));
        LocalDate to = from.plusDays(HORIZON_DAYS - 1L);
        if (!identity.hasActiveRole(info.doctorUserId(), info.clinicId(), "DOCTOR")) {
            return new ScheduleViews.SessionGenerationView(from, to, List.of(), List.of());
        }

        List<StoredShift> activeShifts = shifts.findActiveByDoctorClinicId(info.id());
        List<ScheduleException> activeExceptions = exceptions.findActiveOverlapping(info.id(), from, to)
                .stream().map(StoredException::exception).toList();
        List<BookingSessionInfo> sessions = new ArrayList<>();
        Set<UUID> conflicts = new LinkedHashSet<>();
        for (StoredShift storedShift : activeShifts) {
            ScheduleShift shift = storedShift.shift();
            List<ScheduleSessionPlanner.PlannedSession> planned =
                    ScheduleSessionPlanner.plan(shift, activeExceptions, from, to);
            List<BookingSessionSpecification> specifications = planned.stream()
                    .map(session -> new BookingSessionSpecification(session.sessionDate(),
                            session.startTime(), session.endTime(), session.capacity(),
                            session.followUpQuota()))
                    .toList();
            BookingSessionSyncResult result = bookings.synchronizeSessions(
                    info.id(), shift.id(), specifications, from, to);
            sessions.addAll(result.sessions().stream()
                    .filter(session -> !session.sessionDate().isBefore(from)
                            && !session.sessionDate().isAfter(to))
                    .toList());
            conflicts.addAll(result.conflictingSessionIds());
        }
        sessions.sort(java.util.Comparator.comparing(BookingSessionInfo::sessionDate)
                .thenComparing(BookingSessionInfo::startTime));
        return new ScheduleViews.SessionGenerationView(from, to,
                sessions.stream().map(ScheduleViews.SessionView::from).toList(),
                List.copyOf(conflicts));
    }

    public LocalDate horizonStart(DoctorClinicScheduleInfo info) {
        return LocalDate.now(clock.withZone(zone(info)));
    }

    public LocalDate horizonEnd(DoctorClinicScheduleInfo info) {
        return horizonStart(info).plusDays(HORIZON_DAYS - 1L);
    }

    public ZoneId zone(DoctorClinicScheduleInfo info) {
        try {
            return ZoneId.of(info.clinicTimeZone());
        } catch (RuntimeException exception) {
            throw new SchedulingException(ScheduleError.INVALID_INPUT,
                    "Clinic time zone is invalid: " + info.clinicTimeZone());
        }
    }
}
