//package com.clinic.backend.oldNotImportant.doctorModuleOld.internal;
//
//import org.springframework.data.jpa.repository.JpaRepository;
//import java.util.List;
//import java.util.Optional;
//
//public interface ClinicDoctorConfigRepository extends JpaRepository<ClinicDoctorConfigEntity, Long> {
//    Optional<ClinicDoctorConfigEntity> findByClinicIdAndDoctorId(Long clinicId, Long doctorId);
//    List<ClinicDoctorConfigEntity> findAllByClinicId(Long clinicId);
//}