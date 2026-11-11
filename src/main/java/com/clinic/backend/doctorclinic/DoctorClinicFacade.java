package com.clinic.backend.doctorclinic;

import com.clinic.backend.doctorclinic.internal.application.DoctorClinicApplicationService;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/** Only public entry point for cross-module doctor/clinic relationship queries. */
@Component
public class DoctorClinicFacade {
    private final DoctorClinicApplicationService service;

    public DoctorClinicFacade(DoctorClinicApplicationService service) {
        this.service = service;
    }

    /** Trusted module-to-module lookup used by Billing to read the minimal active price configuration. */
    public Optional<DoctorClinicPricing> findActivePricing(UUID clinicId, UUID doctorId) {
        return service.findActivePricing(clinicId, doctorId);
    }

    public Optional<DoctorClinicScheduleInfo> findScheduleInfo(UUID doctorClinicId) {
        return service.findScheduleInfo(doctorClinicId);
    }

    public List<DoctorClinicScheduleInfo> findAllActiveForScheduling() {
        return service.findAllActiveScheduleInfo();
    }

    public List<DoctorClinicInfo> findForClinic(UUID clinicId, UUID requestingUserId) {
        return service.findForClinic(clinicId, requestingUserId);
    }

    public List<DoctorClinicInfo> findForDoctorUser(UUID doctorUserId) {
        return service.findForDoctorUser(doctorUserId);
    }
}
