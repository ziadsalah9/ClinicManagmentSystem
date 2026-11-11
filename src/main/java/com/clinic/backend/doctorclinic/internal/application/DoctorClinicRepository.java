package com.clinic.backend.doctorclinic.internal.application;

import com.clinic.backend.doctorclinic.internal.domain.DoctorClinic;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface DoctorClinicRepository {
    void lockAssociation(UUID clinicId, UUID doctorId);
    Optional<DoctorClinic> findById(UUID id);
    Optional<DoctorClinic> findByClinicAndDoctor(UUID clinicId, UUID doctorId);
    List<DoctorClinic> findByClinicId(UUID clinicId);
    List<DoctorClinic> findByDoctorId(UUID doctorId);
    List<DoctorClinic> findAllActive();
    DoctorClinic save(DoctorClinic association);
}
