package com.clinic.backend.doctor.internal.application;

import com.clinic.backend.doctor.internal.domain.DoctorProfile;
import com.clinic.backend.doctor.internal.domain.Specialty;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

public interface DoctorRepository {
    void lockProfile(UUID userId);
    Optional<DoctorProfile> findById(UUID doctorId);
    Optional<DoctorProfile> findByUserId(UUID userId);
    List<DoctorProfile> findByIds(Collection<UUID> doctorIds);
    DoctorProfile save(DoctorProfile doctor);
    List<Specialty> findActiveSpecialties();
    Set<UUID> findActiveSpecialtyIds(Set<UUID> ids);
}
