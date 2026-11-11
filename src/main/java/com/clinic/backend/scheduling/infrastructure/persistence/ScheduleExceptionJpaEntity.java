package com.clinic.backend.scheduling.infrastructure.persistence;

import com.clinic.backend.scheduling.domain.model.ScheduleException;
import com.clinic.backend.scheduling.domain.model.ScheduleExceptionType;
import com.clinic.backend.scheduling.domain.model.TimeRange;
import com.clinic.backend.shared.AuditedJpaEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.UUID;

@Entity
@Table(name = "schedule_exception", schema = "scheduling")
public class ScheduleExceptionJpaEntity extends AuditedJpaEntity {
    @Id
    private UUID id;

    @Column(name = "doctor_clinic_id", nullable = false, updatable = false)
    private UUID doctorClinicId;

    @Column(name = "shift_id")
    private UUID shiftId;

    @Enumerated(EnumType.STRING)
    @Column(name = "type", nullable = false, length = 20)
    private ScheduleExceptionType type;

    @Column(name = "date_from", nullable = false)
    private LocalDate dateFrom;

    @Column(name = "date_to", nullable = false)
    private LocalDate dateTo;

    @Column(name = "changed_start_time")
    private LocalTime changedStartTime;

    @Column(name = "changed_end_time")
    private LocalTime changedEndTime;

    @Column(name = "reason", length = 500)
    private String reason;

    @Column(name = "cancelled_at")
    private Instant cancelledAt;

    protected ScheduleExceptionJpaEntity() { }

    public ScheduleExceptionJpaEntity(ScheduleException exception) {
        this.id = exception.id();
        this.doctorClinicId = exception.doctorClinicId();
        apply(exception);
    }

    public void apply(ScheduleException exception) {
        this.shiftId = exception.shiftId();
        this.type = exception.type();
        this.dateFrom = exception.dateFrom();
        this.dateTo = exception.dateTo();
        TimeRange changed = exception.changedWorkingHours();
        this.changedStartTime = changed == null ? null : changed.start();
        this.changedEndTime = changed == null ? null : changed.end();
        this.reason = exception.reason();
        this.cancelledAt = exception.cancelledAt();
    }

    public ScheduleException toDomain() {
        TimeRange changed = changedStartTime == null ? null : new TimeRange(changedStartTime, changedEndTime);
        return ScheduleException.restore(id, doctorClinicId, shiftId, type, dateFrom, dateTo,
                changed, reason, cancelledAt);
    }
}
