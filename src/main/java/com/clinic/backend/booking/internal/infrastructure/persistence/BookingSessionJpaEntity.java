package com.clinic.backend.booking.internal.infrastructure.persistence;

import com.clinic.backend.booking.BookingSessionInfo;
import com.clinic.backend.booking.BookingSessionSpecification;
import com.clinic.backend.shared.AuditedJpaEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.UUID;

@Entity
@Table(name = "session", schema = "booking",
        uniqueConstraints = @UniqueConstraint(name = "uk_booking_session_slot",
                columnNames = {"doctor_clinic_id", "shift_id", "session_date"}))
public class BookingSessionJpaEntity extends AuditedJpaEntity {
    @Id
    private UUID id;

    @Column(name = "doctor_clinic_id", nullable = false, updatable = false)
    private UUID doctorClinicId;

    @Column(name = "shift_id", nullable = false, updatable = false)
    private UUID shiftId;

    @Column(name = "session_date", nullable = false, updatable = false)
    private LocalDate sessionDate;

    @Column(name = "start_time", nullable = false)
    private LocalTime startTime;

    @Column(name = "end_time", nullable = false)
    private LocalTime endTime;

    @Column(name = "capacity", nullable = false)
    private int capacity;

    @Column(name = "booked_count", nullable = false)
    private int bookedCount;

    @Column(name = "follow_up_quota", nullable = false)
    private int followUpQuota;

    @Column(name = "follow_up_booked_count", nullable = false)
    private int followUpBookedCount;

    @Column(name = "status", nullable = false, length = 20)
    private String status;

    protected BookingSessionJpaEntity() { }

    public static BookingSessionJpaEntity create(UUID doctorClinicId, UUID shiftId,
                                                 BookingSessionSpecification specification) {
        BookingSessionJpaEntity entity = new BookingSessionJpaEntity();
        entity.id = UUID.randomUUID();
        entity.doctorClinicId = doctorClinicId;
        entity.shiftId = shiftId;
        entity.sessionDate = specification.sessionDate();
        entity.bookedCount = 0;
        entity.followUpBookedCount = 0;
        entity.status = "ACTIVE";
        entity.applySpecification(specification);
        return entity;
    }

    public void applySpecification(BookingSessionSpecification specification) {
        this.startTime = specification.startTime();
        this.endTime = specification.endTime();
        this.capacity = specification.capacity();
        this.followUpQuota = specification.followUpQuota();
    }

    public void retainBookedSlot(BookingSessionSpecification specification) {
        this.capacity = Math.max(this.capacity, specification.capacity());
        this.followUpQuota = Math.max(this.followUpQuota, specification.followUpQuota());
        this.endTime = specification.endTime().isAfter(this.endTime) ? specification.endTime() : this.endTime;
    }

    public BookingSessionInfo toInfo() {
        return new BookingSessionInfo(id, doctorClinicId, shiftId, sessionDate, startTime, endTime,
                capacity, bookedCount, followUpQuota, followUpBookedCount, status, getVersion(),
                getCreatedAt(), getUpdatedAt());
    }

    public UUID getId() { return id; }
    public UUID getDoctorClinicId() { return doctorClinicId; }
    public UUID getShiftId() { return shiftId; }
    public LocalDate getSessionDate() { return sessionDate; }
    public int getCapacity() { return capacity; }
    public int getBookedCount() { return bookedCount; }
    public int getFollowUpQuota() { return followUpQuota; }
    public int getFollowUpBookedCount() { return followUpBookedCount; }
}
