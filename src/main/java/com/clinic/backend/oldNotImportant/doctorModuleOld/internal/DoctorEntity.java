//package com.clinic.backend.oldNotImportant.doctorModuleOld.internal;
//
//import jakarta.persistence.*;
//import lombok.*;
//import org.springframework.data.annotation.CreatedDate;
//import org.springframework.data.annotation.LastModifiedDate;
//import org.springframework.data.jpa.domain.support.AuditingEntityListener;
//
//import java.time.LocalDateTime;
//
//@Entity
//@Table(name = "doctors")
//@EntityListeners(AuditingEntityListener.class)
//@Data
//@NoArgsConstructor
//@AllArgsConstructor
//public class DoctorEntity {
//
//    @Id
//    @GeneratedValue(strategy = GenerationType.IDENTITY)
//    private Long id;
//
//    @Column(nullable = false)
//    private String name;
//
//    @Column(nullable = false)
//    private String specialty;
//
//
//    @Column(name = "license_number", unique = true)
//    private String licenseNumber; // رقم ترخيص مزاولة المهنة / رقم النقابة
//
//    @Column(nullable = false, unique = true)
//    private String phone; // رقم الموبايل الأساسي للتواصل
//
//    @Column(nullable = false)
//    private String status; // ACTIVE, INACTIVE
//
//    @CreatedDate
//    @Column(name = "created_at", updatable = false)
//    private LocalDateTime createdAt;
//
//    @LastModifiedDate
//    @Column(name = "updated_at")
//    private LocalDateTime updatedAt;
//}