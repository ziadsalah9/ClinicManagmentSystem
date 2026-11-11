package com.clinic.backend.scheduling.application;

import com.clinic.backend.booking.BookingFacade;
import com.clinic.backend.booking.BookingSessionInfo;
import com.clinic.backend.doctorclinic.DoctorClinicFacade;
import com.clinic.backend.doctorclinic.DoctorClinicScheduleInfo;
import com.clinic.backend.identity.IdentityFacade;
import com.clinic.backend.scheduling.application.ScheduleExceptionRepository.StoredException;
import com.clinic.backend.scheduling.application.ScheduleShiftRepository.StoredShift;
import com.clinic.backend.scheduling.domain.ScheduleError;
import com.clinic.backend.scheduling.domain.SchedulingException;
import com.clinic.backend.scheduling.domain.model.CapacityMode;
import com.clinic.backend.scheduling.domain.model.ScheduleConflict;
import com.clinic.backend.scheduling.domain.model.ScheduleException;
import com.clinic.backend.scheduling.domain.model.ScheduleExceptionType;
import com.clinic.backend.scheduling.domain.model.ScheduleShift;
import com.clinic.backend.scheduling.domain.model.ShiftBreak;
import com.clinic.backend.scheduling.domain.model.TimeRange;
import com.clinic.backend.scheduling.domain.model.WeeklyShiftRule;
import com.clinic.backend.scheduling.domain.service.ScheduleConflictDetector;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.DayOfWeek;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

@Service
public class ScheduleApplicationService {
    private final ScheduleShiftRepository shifts;
    private final ScheduleExceptionRepository exceptions;
    private final ScheduleGuardRepository guards;
    private final DoctorClinicFacade doctorClinics;
    private final IdentityFacade identity;
    private final BookingFacade bookings;
    private final ScheduleHorizonService horizon;
    private final Clock clock;

    public ScheduleApplicationService(ScheduleShiftRepository shifts,
                                      ScheduleExceptionRepository exceptions,
                                      ScheduleGuardRepository guards,
                                      DoctorClinicFacade doctorClinics,
                                      IdentityFacade identity,
                                      BookingFacade bookings,
                                      ScheduleHorizonService horizon,
                                      Clock clock) {
        this.shifts = shifts;
        this.exceptions = exceptions;
        this.guards = guards;
        this.doctorClinics = doctorClinics;
        this.identity = identity;
        this.bookings = bookings;
        this.horizon = horizon;
        this.clock = clock;
    }

    @Transactional(readOnly = true)
    public List<ScheduleViews.ShiftView> listShifts(UUID actorUserId, UUID clinicId, UUID doctorClinicId) {
        DoctorClinicScheduleInfo info = requireManage(actorUserId, clinicId, doctorClinicId);
        return shifts.findByDoctorClinicId(info.id()).stream()
                .map(ScheduleViews.ShiftView::from).toList();
    }

    @Transactional
    public ScheduleViews.ShiftView createShift(UUID actorUserId, UUID clinicId, UUID doctorClinicId,
                                               ScheduleInputs.ShiftInput input) {
        DoctorClinicScheduleInfo info = requireManage(actorUserId, clinicId, doctorClinicId);
        ScheduleShift candidate = buildShift(info.id(), input);
        guards.lockDoctorClinic(info.id());
        ensureNameAvailable(info.id(), candidate.name(), null);
        ensureNoConflicts(candidate, null);
        StoredShift saved = shifts.save(candidate);
        if (candidate.active()) {
            horizon.reconcile(info);
        }
        return ScheduleViews.ShiftView.from(shifts.findById(saved.shift().id()).orElse(saved));
    }

    @Transactional
    public ScheduleViews.ShiftView updateShift(UUID actorUserId, UUID clinicId, UUID doctorClinicId,
                                               UUID shiftId, ScheduleInputs.ShiftInput input,
                                               Long expectedVersion) {
        DoctorClinicScheduleInfo info = requireManage(actorUserId, clinicId, doctorClinicId);
        guards.lockDoctorClinic(info.id());
        StoredShift current = requireShift(info.id(), shiftId);
        requireVersion(expectedVersion, current.version());
        ScheduleShift candidate = buildUpdatedShift(current.shift(), input);
        ensureNameAvailable(info.id(), candidate.name(), current.shift().id());
        ensureNoConflicts(candidate, current.shift().id());
        ensureNoBookingBlockingChanges(info, candidate);
        StoredShift saved = shifts.save(candidate);
        if (candidate.active()) {
            horizon.reconcile(info);
        }
        return ScheduleViews.ShiftView.from(shifts.findById(saved.shift().id()).orElse(saved));
    }

    @Transactional
    public ScheduleViews.ShiftView setShiftActivation(UUID actorUserId, UUID clinicId, UUID doctorClinicId,
                                                      UUID shiftId, ScheduleInputs.ActivationInput input) {
        DoctorClinicScheduleInfo info = requireManage(actorUserId, clinicId, doctorClinicId);
        guards.lockDoctorClinic(info.id());
        StoredShift current = requireShift(info.id(), shiftId);
        requireVersion(input.expectedVersion(), current.version());
        ScheduleShift candidate = input.active() ? current.shift().activate() : current.shift().deactivate();
        if (input.active()) {
            ensureNoConflicts(candidate, candidate.id());
        }
        StoredShift saved = shifts.save(candidate);
        if (input.active()) {
            horizon.reconcile(info);
        }
        return ScheduleViews.ShiftView.from(shifts.findById(saved.shift().id()).orElse(saved));
    }

    @Transactional(readOnly = true)
    public ScheduleViews.CapacityPreviewView previewCapacity(UUID actorUserId, UUID clinicId, UUID doctorClinicId,
                                                             ScheduleInputs.ShiftInput input) {
        DoctorClinicScheduleInfo info = requireManage(actorUserId, clinicId, doctorClinicId);
        ScheduleShift candidate = buildShift(info.id(), input);
        LocalDate from = horizon.horizonStart(info);
        LocalDate to = horizon.horizonEnd(info);
        List<ScheduleException> activeExceptions = exceptions.findActiveOverlapping(info.id(), from, to)
                .stream().map(StoredException::exception).toList();
        List<ScheduleViews.CapacityDayView> days = ScheduleSessionPlanner.plan(candidate, activeExceptions, from, to)
                .stream()
                .map(session -> new ScheduleViews.CapacityDayView(session.sessionDate(),
                        session.startTime(), session.endTime(), session.capacity(), session.followUpQuota()))
                .toList();
        return new ScheduleViews.CapacityPreviewView(from, to, days);
    }

    @Transactional(readOnly = true)
    public ScheduleViews.ChangePreviewView previewChange(UUID actorUserId, UUID clinicId, UUID doctorClinicId,
                                                         UUID shiftId, ScheduleInputs.ShiftInput input) {
        DoctorClinicScheduleInfo info = requireManage(actorUserId, clinicId, doctorClinicId);
        StoredShift current = requireShift(info.id(), shiftId);
        ScheduleShift candidate = buildUpdatedShift(current.shift(), input);
        List<ScheduleConflict> conflicts = ScheduleConflictDetector.detectConflicts(candidate,
                activeOtherShifts(info.id(), candidate.id()));
        List<ScheduleViews.ConflictView> conflictViews = conflicts.stream()
                .map(ScheduleViews.ConflictView::from).toList();

        LocalDate from = horizon.horizonStart(info);
        LocalDate to = horizon.horizonEnd(info);
        List<ScheduleException> activeExceptions = exceptions.findActiveOverlapping(info.id(), from, to)
                .stream().map(StoredException::exception).toList();
        List<ScheduleSessionPlanner.PlannedSession> planned =
                ScheduleSessionPlanner.plan(candidate, activeExceptions, from, to);
        Map<LocalDate, ScheduleSessionPlanner.PlannedSession> plannedByDate = new HashMap<>();
        for (ScheduleSessionPlanner.PlannedSession session : planned) {
            plannedByDate.put(session.sessionDate(), session);
        }
        Map<LocalDate, BookingSessionInfo> existingByDate = new HashMap<>();
        for (BookingSessionInfo session : bookings.findSessions(info.id(), from, to)) {
            if (session.shiftId().equals(candidate.id())) {
                existingByDate.put(session.sessionDate(), session);
            }
        }

        List<ScheduleViews.AffectedSessionView> affected = new ArrayList<>();
        for (ScheduleSessionPlanner.PlannedSession session : planned) {
            BookingSessionInfo existing = existingByDate.get(session.sessionDate());
            if (existing == null) {
                affected.add(new ScheduleViews.AffectedSessionView(null, session.sessionDate(),
                        "CREATE", 0, 0, true, "A new session will be generated"));
                continue;
            }
            boolean changed = !session.startTime().equals(existing.startTime())
                    || !session.endTime().equals(existing.endTime())
                    || session.capacity() != existing.capacity()
                    || session.followUpQuota() != existing.followUpQuota();
            if (!changed) {
                continue;
            }
            boolean booked = existing.bookedCount() > 0 || existing.followUpBookedCount() > 0;
            boolean safe = !booked || (session.startTime().equals(existing.startTime())
                    && !session.endTime().isBefore(existing.endTime())
                    && session.capacity() >= existing.bookedCount()
                    && session.followUpQuota() >= existing.followUpBookedCount());
            String reason = safe
                    ? (booked ? "Booked session remains valid and is updated only where safe"
                    : "Generated session can be updated safely")
                    : "Existing bookings exceed the proposed capacity, follow-up quota, or working hours";
            affected.add(new ScheduleViews.AffectedSessionView(existing.id(), session.sessionDate(),
                    "UPDATE", existing.bookedCount(), existing.followUpBookedCount(), safe, reason));
        }
        for (BookingSessionInfo existing : existingByDate.values()) {
            if (plannedByDate.containsKey(existing.sessionDate())) {
                continue;
            }
            boolean booked = existing.bookedCount() > 0 || existing.followUpBookedCount() > 0;
            affected.add(new ScheduleViews.AffectedSessionView(existing.id(), existing.sessionDate(),
                    "REMOVE", existing.bookedCount(), existing.followUpBookedCount(), !booked,
                    booked ? "Booked sessions are retained and must be handled manually"
                            : "Unbooked generated session can be removed"));
        }
        affected.sort(Comparator.comparing(ScheduleViews.AffectedSessionView::date));
        boolean blocking = !conflictViews.isEmpty() || affected.stream().anyMatch(view -> !view.safeToApply());
        return new ScheduleViews.ChangePreviewView(affected, blocking, conflictViews);
    }

    @Transactional
    public ScheduleViews.ExceptionView createException(UUID actorUserId, UUID clinicId, UUID doctorClinicId,
                                                       ScheduleInputs.ExceptionInput input) {
        DoctorClinicScheduleInfo info = requireManage(actorUserId, clinicId, doctorClinicId);
        if (input.shiftId() != null) {
            requireShift(info.id(), input.shiftId());
        }
        ScheduleException exception = buildException(info.id(), input);
        guards.lockDoctorClinic(info.id());
        StoredException saved = exceptions.save(exception);
        horizon.reconcile(info);
        return ScheduleViews.ExceptionView.from(exceptions.findById(saved.exception().id()).orElse(saved));
    }

    @Transactional(readOnly = true)
    public List<ScheduleViews.ExceptionView> listExceptions(UUID actorUserId, UUID clinicId, UUID doctorClinicId) {
        DoctorClinicScheduleInfo info = requireManage(actorUserId, clinicId, doctorClinicId);
        return exceptions.findByDoctorClinicId(info.id()).stream()
                .map(ScheduleViews.ExceptionView::from).toList();
    }

    @Transactional
    public ScheduleViews.ExceptionView cancelException(UUID actorUserId, UUID clinicId, UUID doctorClinicId,
                                                       UUID exceptionId,
                                                       ScheduleInputs.CancelExceptionInput input) {
        DoctorClinicScheduleInfo info = requireManage(actorUserId, clinicId, doctorClinicId);
        guards.lockDoctorClinic(info.id());
        StoredException stored = exceptions.findById(exceptionId)
                .filter(candidate -> candidate.exception().doctorClinicId().equals(info.id()))
                .orElseThrow(() -> new SchedulingException(ScheduleError.EXCEPTION_NOT_FOUND,
                        "Schedule exception was not found"));
        if (input != null && input.expectedVersion() != null) {
            requireVersion(input.expectedVersion(), stored.version());
        }
        if (stored.exception().isCancelled()) {
            return ScheduleViews.ExceptionView.from(stored);
        }
        StoredException saved = exceptions.save(stored.exception().cancel(Instant.now(clock)));
        horizon.reconcile(info);
        return ScheduleViews.ExceptionView.from(exceptions.findById(saved.exception().id()).orElse(saved));
    }

    @Transactional
    public ScheduleViews.SessionGenerationView generateSessions(UUID actorUserId, UUID clinicId,
                                                                UUID doctorClinicId) {
        DoctorClinicScheduleInfo info = requireManage(actorUserId, clinicId, doctorClinicId);
        return horizon.generate(info.id());
    }

    @Transactional(readOnly = true)
    public List<ScheduleViews.SessionView> findSessions(UUID actorUserId, UUID clinicId, UUID doctorClinicId,
                                                        LocalDate from, LocalDate to) {
        DoctorClinicScheduleInfo info = requireRead(actorUserId, clinicId, doctorClinicId);
        if (from == null || to == null || from.isAfter(to)) {
            throw new SchedulingException(ScheduleError.INVALID_DATE_RANGE,
                    "The from date must be on or before the to date");
        }
        if (to.isAfter(from.plusDays(366))) {
            throw new SchedulingException(ScheduleError.INVALID_DATE_RANGE,
                    "Session queries are limited to 367 days");
        }
        return bookings.findSessions(info.id(), from, to).stream()
                .map(ScheduleViews.SessionView::from).toList();
    }

    private DoctorClinicScheduleInfo requireManage(UUID actorUserId, UUID clinicId, UUID doctorClinicId) {
        DoctorClinicScheduleInfo info = requireAssociation(clinicId, doctorClinicId);
        if (!identity.hasPermission(actorUserId, clinicId, "SCHEDULE_MANAGE")) {
            throw new SchedulingException(ScheduleError.ACCESS_DENIED,
                    "SCHEDULE_MANAGE permission is required to manage this doctor's schedule");
        }
        return info;
    }

    private DoctorClinicScheduleInfo requireRead(UUID actorUserId, UUID clinicId, UUID doctorClinicId) {
        DoctorClinicScheduleInfo info = requireAssociation(clinicId, doctorClinicId);
        boolean manager = identity.hasPermission(actorUserId, clinicId, "SCHEDULE_MANAGE");
        boolean ownDoctor = info.doctorUserId().equals(actorUserId)
                && identity.hasActiveRole(actorUserId, clinicId, "DOCTOR");
        if (!manager && !ownDoctor) {
            throw new SchedulingException(ScheduleError.ACCESS_DENIED,
                    "Only clinic schedule managers or the doctor can read these sessions");
        }
        return info;
    }

    private DoctorClinicScheduleInfo requireAssociation(UUID clinicId, UUID doctorClinicId) {
        DoctorClinicScheduleInfo info = doctorClinics.findScheduleInfo(doctorClinicId)
                .orElseThrow(() -> new SchedulingException(ScheduleError.DOCTOR_CLINIC_NOT_FOUND,
                        "Doctor-clinic association was not found"));
        if (!info.clinicId().equals(clinicId)) {
            throw new SchedulingException(ScheduleError.DOCTOR_CLINIC_NOT_FOUND,
                    "Doctor-clinic association was not found in this clinic");
        }
        return info;
    }

    private StoredShift requireShift(UUID doctorClinicId, UUID shiftId) {
        return shifts.findById(shiftId)
                .filter(stored -> stored.shift().doctorClinicId().equals(doctorClinicId))
                .orElseThrow(() -> new SchedulingException(ScheduleError.SHIFT_NOT_FOUND,
                        "Shift was not found"));
    }

    private ScheduleShift buildShift(UUID doctorClinicId, ScheduleInputs.ShiftInput input) {
        try {
            return ScheduleShift.create(doctorClinicId, input.name(), input.capacityMode(),
                    input.averageVisitMinutes(), input.fixedCapacity(), input.effectiveFrom(),
                    mapRules(input.weeklyRules()));
        } catch (NullPointerException | IllegalArgumentException exception) {
            throw new SchedulingException(ScheduleError.INVALID_INPUT,
                    "Invalid shift configuration: " + exception.getMessage());
        }
    }

    private ScheduleShift buildUpdatedShift(ScheduleShift current, ScheduleInputs.ShiftInput input) {
        try {
            return current.updateConfiguration(input.name(), input.capacityMode(),
                    input.averageVisitMinutes(), input.fixedCapacity(), input.effectiveFrom(),
                    mapRules(input.weeklyRules()));
        } catch (NullPointerException | IllegalArgumentException exception) {
            throw new SchedulingException(ScheduleError.INVALID_INPUT,
                    "Invalid shift configuration: " + exception.getMessage());
        }
    }

    private List<WeeklyShiftRule> mapRules(List<ScheduleInputs.WeeklyRuleInput> rules) {
        if (rules == null) {
            throw new SchedulingException(ScheduleError.INVALID_INPUT, "Weekly rules are required");
        }
        try {
            return rules.stream().map(rule -> {
                List<ShiftBreak> breaks = rule.breaks() == null ? List.of() : rule.breaks().stream()
                        .map(shiftBreak -> new ShiftBreak(new TimeRange(shiftBreak.startTime(),
                                shiftBreak.endTime())))
                        .toList();
                return new WeeklyShiftRule(rule.dayOfWeek(),
                        new TimeRange(rule.startTime(), rule.endTime()),
                        rule.followUpQuota(), breaks);
            }).toList();
        } catch (NullPointerException | IllegalArgumentException exception) {
            throw new SchedulingException(ScheduleError.INVALID_INPUT,
                    "Invalid shift configuration: " + exception.getMessage());
        }
    }

    private void ensureNameAvailable(UUID doctorClinicId, String name, UUID excludedShiftId) {
        boolean exists = shifts.findByDoctorClinicId(doctorClinicId).stream()
                .filter(stored -> excludedShiftId == null || !stored.shift().id().equals(excludedShiftId))
                .anyMatch(stored -> stored.shift().name().equalsIgnoreCase(name));
        if (exists) {
            throw new SchedulingException(ScheduleError.SHIFT_NAME_CONFLICT,
                    "A shift with this name already exists for this doctor");
        }
    }

    private void ensureNoBookingBlockingChanges(DoctorClinicScheduleInfo info, ScheduleShift candidate) {
        if (!candidate.active()) {
            return;
        }
        LocalDate from = horizon.horizonStart(info);
        LocalDate to = horizon.horizonEnd(info);
        List<ScheduleException> activeExceptions = exceptions.findActiveOverlapping(info.id(), from, to)
                .stream().map(StoredException::exception).toList();
        Map<LocalDate, ScheduleSessionPlanner.PlannedSession> plannedByDate = new HashMap<>();
        for (ScheduleSessionPlanner.PlannedSession planned
                : ScheduleSessionPlanner.plan(candidate, activeExceptions, from, to)) {
            plannedByDate.put(planned.sessionDate(), planned);
        }
        for (BookingSessionInfo existing : bookings.findSessions(info.id(), from, to)) {
            if (!existing.shiftId().equals(candidate.id())
                    || (existing.bookedCount() == 0 && existing.followUpBookedCount() == 0)) {
                continue;
            }
            ScheduleSessionPlanner.PlannedSession planned = plannedByDate.get(existing.sessionDate());
            if (planned == null) {
                throw bookingConflict(existing, "would remove a booked session");
            }
            if (!planned.startTime().equals(existing.startTime())
                    || planned.endTime().isBefore(existing.endTime())
                    || planned.capacity() < existing.bookedCount()
                    || planned.followUpQuota() < existing.followUpBookedCount()) {
                throw bookingConflict(existing, "would invalidate existing bookings");
            }
        }
    }

    private static SchedulingException bookingConflict(BookingSessionInfo session, String reason) {
        return new SchedulingException(ScheduleError.BOOKING_CONFLICT,
                "The schedule change " + reason + " on " + session.sessionDate()
                        + ". Resolve bookings first.");
    }

    private void ensureNoConflicts(ScheduleShift candidate, UUID excludedShiftId) {
        List<ScheduleConflict> conflicts = ScheduleConflictDetector.detectConflicts(candidate,
                activeOtherShifts(candidate.doctorClinicId(), excludedShiftId));
        if (!conflicts.isEmpty()) {
            ScheduleConflict conflict = conflicts.getFirst();
            throw new SchedulingException(ScheduleError.SHIFT_CONFLICT,
                    "Shift overlaps another active shift on " + conflict.dayOfWeek()
                            + " between " + conflict.overlappingPeriod().start()
                            + " and " + conflict.overlappingPeriod().end());
        }
    }

    private List<ScheduleShift> activeOtherShifts(UUID doctorClinicId, UUID excludedShiftId) {
        return shifts.findActiveByDoctorClinicId(doctorClinicId).stream()
                .map(StoredShift::shift)
                .filter(shift -> excludedShiftId == null || !shift.id().equals(excludedShiftId))
                .toList();
    }

    private static void requireVersion(Long expectedVersion, long actualVersion) {
        if (expectedVersion == null || expectedVersion != actualVersion) {
            throw new SchedulingException(ScheduleError.STALE_VERSION,
                    "The schedule changed. Reload the latest data before retrying.");
        }
    }

    private ScheduleException buildException(UUID doctorClinicId, ScheduleInputs.ExceptionInput input) {
        Objects.requireNonNull(input, "Exception input is required");
        ScheduleExceptionType type = input.type();
        TimeRange changed = input.startTime() == null && input.endTime() == null
                ? null : new TimeRange(input.startTime(), input.endTime());
        try {
            return ScheduleException.create(doctorClinicId, input.shiftId(), type,
                    input.dateFrom(), input.dateTo(), changed, input.reason());
        } catch (NullPointerException | IllegalArgumentException exception) {
            throw new SchedulingException(ScheduleError.INVALID_INPUT,
                    "Invalid schedule exception: " + exception.getMessage());
        }
    }
}
