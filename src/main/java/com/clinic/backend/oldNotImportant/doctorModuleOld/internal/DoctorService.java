//package com.clinic.backend.doctorModule.internal;
//
//import com.clinic.backend.clinicModule.clinicFacade;
//import com.clinic.backend.clinicModule.clinicFacade; // استدعاء الفاساد الخاص بموديول العيادة فقط
//import lombok.RequiredArgsConstructor;
//import org.springframework.stereotype.Service;
//import org.springframework.transaction.annotation.Transactional;
//
//import java.math.BigDecimal;
//
//@Service
//@RequiredArgsConstructor
//public class DoctorService {
//
//    private final DoctorRepository doctorRepository;
//    private final ClinicDoctorConfigRepository configRepository;
//    private final clinicFacade _clinicFacade; // البوابة الرسمية للتحقق من العيادة
//
//    @Transactional
//    public DoctorEntity registerDoctor(String name, String specialty, String licenseNumber, String phone) {
//        if (doctorRepository.existsByPhone(phone)) {
//            throw new IllegalArgumentException("رقم الهاتـف مسجل مسبقاً لطبيب آخر.");
//        }
//        if (licenseNumber != null && doctorRepository.existsByLicenseNumber(licenseNumber)) {
//            throw new IllegalArgumentException("رقم الترخيص مسجل مسبقاً.");
//        }
//
//        DoctorEntity doctor = new DoctorEntity();
//        doctor.setName(name);
//        doctor.setSpecialty(specialty);
//        doctor.setLicenseNumber(licenseNumber);
//        doctor.setPhone(phone);
//        doctor.setStatus("ACTIVE");
//
//        return doctorRepository.save(doctor);
//    }
//
//    @Transactional
//    public ClinicDoctorConfigEntity assignDoctorToClinic(
//            Long clinicId, Long doctorId, BigDecimal consultationFee, BigDecimal followUpFee, String instapayAddress) {
//
//        // 1. التحقق من وجود العيادة عبر الـ ClinicFacade (احترام حدود الموديولات)
//        boolean clinicExists = clinicFacade.validateClinicExists(clinicId);
//        if (!clinicExists) {
//            throw new IllegalArgumentException("العيادة غير موجودة أو غير مفعلة.");
//        }
//
//        // 2. جلب الدكتور
//        DoctorEntity doctor = doctorRepository.findById(doctorId)
//                .orElseThrow(() -> new RuntimeException("الطبيب غير موجود برقم: " + doctorId));
//
//        // 3. التحقق هل تم ربطه بالعيادة من قبل؟
//        configRepository.findByClinicIdAndDoctorId(clinicId, doctorId).ifPresent(c -> {
//            throw new IllegalStateException("هذا الطبيب مضاف بالفعل لهذه العيادة.");
//        });
//
//        // 4. إنشاء وتخزين إعدادات العيادة للطبيب
//        ClinicDoctorConfigEntity config = new ClinicDoctorConfigEntity();
//        config.setClinicId(clinicId);
//        config.setDoctor(doctor);
//        config.setConsultationFee(consultationFee);
//        config.setFollowUpFee(followUpFee);
//        config.setInstapayAddress(instapayAddress);
//
//        return configRepository.save(config);
//    }
//}