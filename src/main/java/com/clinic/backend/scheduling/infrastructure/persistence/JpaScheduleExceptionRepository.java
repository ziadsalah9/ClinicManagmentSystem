package com.clinic.backend.scheduling.infrastructure.persistence;

import com.clinic.backend.scheduling.application.ScheduleExceptionRepository;
import com.clinic.backend.scheduling.domain.model.ScheduleException;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
@Transactional(readOnly = true)
public class JpaScheduleExceptionRepository implements ScheduleExceptionRepository {
    private final SpringDataScheduleExceptionRepository exceptions;

    public JpaScheduleExceptionRepository(SpringDataScheduleExceptionRepository exceptions) {
        this.exceptions = exceptions;
    }

    @Override
    public Optional<StoredException> findById(UUID exceptionId) {
        return exceptions.findById(exceptionId).map(this::snapshot);
    }

    @Override
    public List<StoredException> findByDoctorClinicId(UUID doctorClinicId) {
        return exceptions.findAllByDoctorClinicIdOrderByDateFromAscDateToAsc(doctorClinicId).stream()
                .map(this::snapshot).toList();
    }

    @Override
    public List<StoredException> findActiveOverlapping(UUID doctorClinicId, LocalDate from, LocalDate to) {
        return exceptions.findAllByDoctorClinicIdAndCancelledAtIsNullAndDateFromLessThanEqualAndDateToGreaterThanEqualOrderByDateFromAsc(
                        doctorClinicId, to, from).stream()
                .map(this::snapshot).toList();
    }

    @Override
    @Transactional
    public StoredException save(ScheduleException exception) {
        ScheduleExceptionJpaEntity entity = exceptions.findById(exception.id()).orElse(null);
        if (entity == null) {
            entity = new ScheduleExceptionJpaEntity(exception);
        } else {
            entity.apply(exception);
        }
        return snapshot(exceptions.saveAndFlush(entity));
    }

    private StoredException snapshot(ScheduleExceptionJpaEntity entity) {
        return new StoredException(entity.toDomain(), entity.getVersion(),
                entity.getCreatedAt(), entity.getUpdatedAt());
    }
}
