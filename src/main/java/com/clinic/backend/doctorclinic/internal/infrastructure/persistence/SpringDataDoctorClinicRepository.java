package com.clinic.backend.doctorclinic.internal.infrastructure.persistence;

import com.clinic.backend.doctorclinic.internal.domain.DoctorClinicStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface SpringDataDoctorClinicRepository extends JpaRepository<DoctorClinicJpaEntity, UUID> {
    Optional<DoctorClinicJpaEntity> findByClinicIdAndDoctorId(UUID clinicId, UUID doctorId);
    List<DoctorClinicJpaEntity> findAllByClinicIdAndStatusOrderByDoctorId(UUID clinicId, DoctorClinicStatus status);
    List<DoctorClinicJpaEntity> findAllByDoctorIdAndStatusOrderByClinicId(UUID doctorId, DoctorClinicStatus status);
    List<DoctorClinicJpaEntity> findAllByStatusOrderByClinicIdAscDoctorIdAsc(DoctorClinicStatus status);
}
