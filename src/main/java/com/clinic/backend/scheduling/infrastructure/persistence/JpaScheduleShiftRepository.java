package com.clinic.backend.scheduling.infrastructure.persistence;

import com.clinic.backend.scheduling.application.ScheduleShiftRepository;
import com.clinic.backend.scheduling.domain.model.ScheduleShift;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
@Transactional(readOnly = true)
public class JpaScheduleShiftRepository implements ScheduleShiftRepository {
    private final SpringDataScheduleShiftRepository shifts;

    public JpaScheduleShiftRepository(SpringDataScheduleShiftRepository shifts) {
        this.shifts = shifts;
    }

    @Override
    public Optional<StoredShift> findById(UUID shiftId) {
        return shifts.findById(shiftId).map(this::snapshot);
    }

    @Override
    public List<StoredShift> findByDoctorClinicId(UUID doctorClinicId) {
        return shifts.findAllByDoctorClinicIdOrderByNameAsc(doctorClinicId).stream()
                .map(this::snapshot).toList();
    }

    @Override
    public List<StoredShift> findActiveByDoctorClinicId(UUID doctorClinicId) {
        return shifts.findAllByDoctorClinicIdAndActiveOrderByNameAsc(doctorClinicId, true).stream()
                .map(this::snapshot).toList();
    }

    @Override
    @Transactional
    public StoredShift save(ScheduleShift shift) {
        ScheduleShiftJpaEntity entity = shifts.findById(shift.id()).orElse(null);
        if (entity == null) {
            entity = new ScheduleShiftJpaEntity(shift);
        } else {
            entity.apply(shift);
        }
        return snapshot(shifts.saveAndFlush(entity));
    }

    private StoredShift snapshot(ScheduleShiftJpaEntity entity) {
        return new StoredShift(entity.toDomain(), entity.getVersion(),
                entity.getCreatedAt(), entity.getUpdatedAt());
    }
}
