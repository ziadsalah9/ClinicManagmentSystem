//package com.clinic.backend.doctorModuleOld.dtos;
//
//import java.math.BigDecimal;
//
//public class DoctorDtosOld {
//
//    // DTO لبيانات الطبيب العامة
//    public record DoctorResponse(
//            Long id,
//            String name,
//            String specialty,
//            String licenseNumber,
//            String phone,
//            String status
//    ) {}
//
//    // DTO لإعدادات الطبيب في العيادة (تشمل إنستاباي والأسعار)
//    public record ClinicDoctorConfigResponse(
//            Long id,
//            Long clinicId,
//            DoctorResponse doctor,
//            BigDecimal consultationFee,
//            BigDecimal followUpFee,
//            String instapayAddress
//    ) {}
//}