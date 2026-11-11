package com.clinic.backend.scheduling.infrastructure.persistence;

import com.clinic.backend.scheduling.application.ScheduleGuardRepository;
import jakarta.persistence.EntityManager;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Repository
public class JpaScheduleGuardRepository implements ScheduleGuardRepository {
    private final EntityManager entityManager;

    public JpaScheduleGuardRepository(EntityManager entityManager) {
        this.entityManager = entityManager;
    }

    @Override
    @Transactional(propagation = Propagation.MANDATORY)
    public void lockDoctorClinic(UUID doctorClinicId) {
        entityManager.createNativeQuery("""
                        insert into scheduling.schedule_guard (doctor_clinic_id, updated_at)
                        values (:doctorClinicId, now())
                        on conflict (doctor_clinic_id) do nothing
                        """)
                .setParameter("doctorClinicId", doctorClinicId)
                .executeUpdate();
        entityManager.createNativeQuery("""
                        select doctor_clinic_id
                        from scheduling.schedule_guard
                        where doctor_clinic_id = :doctorClinicId
                        for update
                        """)
                .setParameter("doctorClinicId", doctorClinicId)
                .getSingleResult();
    }
}
