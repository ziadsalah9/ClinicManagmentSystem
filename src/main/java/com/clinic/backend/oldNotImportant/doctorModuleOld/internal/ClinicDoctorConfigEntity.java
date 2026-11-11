//package com.clinic.backend.oldNotImportant.doctorModuleOld.internal;
//
//import jakarta.persistence.*;
//import lombok.Getter;
//import lombok.Setter;
//import lombok.NoArgsConstructor;
//import lombok.AllArgsConstructor;
//
//import java.math.BigDecimal;
//
//@Entity
//@Table(name = "clinic_doctor_configs", uniqueConstraints = {
//    @UniqueConstraint(columnNames = {"clinic_id", "doctor_id"}) // الدكتور يُعَّرَف مرة واحدة فقط كإعدادات داخل نفس العيادة
//})
//@Getter @Setter
//@NoArgsConstructor
//@AllArgsConstructor
//public class ClinicDoctorConfigEntity {
//
//    @Id
//    @GeneratedValue(strategy = GenerationType.IDENTITY)
//    private Long id;
//
//    @Column(name = "clinic_id", nullable = false)
//    private Long clinicId; // مرجع لـ العيادة (من موديول clinic)
//
//    @ManyToOne(fetch = FetchType.LAZY)
//    @JoinColumn(name = "doctor_id", nullable = false)
//    private DoctorEntity doctor;
//
//    @Column(name = "consultation_fee", nullable = false)
//    private BigDecimal consultationFee; // سعر الكشف
//
//    @Column(name = "follow_up_fee")
//    private BigDecimal followUpFee;   //optional
//
//    @Column(name = "instapay_address", nullable = false)
//    private String instapayAddress;
//
//}