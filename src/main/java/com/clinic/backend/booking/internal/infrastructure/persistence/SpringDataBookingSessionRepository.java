package com.clinic.backend.booking.internal.infrastructure.persistence;

import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface SpringDataBookingSessionRepository extends JpaRepository<BookingSessionJpaEntity, UUID> {
    List<BookingSessionJpaEntity> findAllByDoctorClinicIdAndSessionDateBetweenOrderBySessionDateAscStartTimeAsc(
            UUID doctorClinicId, LocalDate from, LocalDate to);

    List<BookingSessionJpaEntity> findAllByDoctorClinicIdAndShiftIdOrderBySessionDateAsc(
            UUID doctorClinicId, UUID shiftId);

    Optional<BookingSessionJpaEntity> findByDoctorClinicIdAndShiftIdAndSessionDate(
            UUID doctorClinicId, UUID shiftId, LocalDate sessionDate);
}
