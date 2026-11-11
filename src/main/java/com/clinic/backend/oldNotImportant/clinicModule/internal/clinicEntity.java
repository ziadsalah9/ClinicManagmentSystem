//package com.clinic.backend.clinicModule.internal;
//import jakarta.persistence.*;
//import lombok.*;
//import org.springframework.data.annotation.CreatedDate;
//import org.springframework.data.annotation.LastModifiedDate;
//import org.springframework.data.jpa.domain.support.AuditingEntityListener;
//
//import java.time.LocalDateTime;
//
//@Entity
//@Table(name = "clinics")
//@EntityListeners(AuditingEntityListener.class) // لتفعيل تتبع الإنشاء والتعديل
//@Data
//@NoArgsConstructor
//@AllArgsConstructor
//public class clinicEntity {
//
//    @Id
//    @GeneratedValue(strategy = GenerationType.IDENTITY)
//    private Long id;
//
//    @Column(nullable = false, unique = true)
//    private String name;
//
//    @Column(nullable = false)
//    private String address;
//
//    private String city;
//
//    @Column(nullable = false)
//    private String status; // ACTIVE, SUSPENDED
//
//    @CreatedDate
//    @Column(name = "created_at", updatable = false)
//    private LocalDateTime createdAt;
//
//    @LastModifiedDate
//    @Column(name = "updated_at")
//    private LocalDateTime updatedAt;
//
//}