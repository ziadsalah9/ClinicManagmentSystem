package com.clinic.backend.doctorclinic.internal.presentation;

import com.clinic.backend.doctorclinic.internal.application.DoctorClinicApplicationService;
import com.clinic.backend.doctorclinic.internal.application.DoctorClinicCommands;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1")
class DoctorClinicController {
    private final DoctorClinicApplicationService doctorClinics;

    DoctorClinicController(DoctorClinicApplicationService doctorClinics) {
        this.doctorClinics = doctorClinics;
    }

    @PutMapping("/clinics/{clinicId}/doctors/{doctorId}")
    @ResponseStatus(HttpStatus.OK)
    DoctorClinicApiModels.DoctorClinicResponse update(
            @AuthenticationPrincipal Jwt jwt,
            @PathVariable UUID clinicId,
            @PathVariable UUID doctorId,
            @Valid @RequestBody DoctorClinicApiModels.UpdateDoctorClinicRequest request
    ) {
        return DoctorClinicApiModels.DoctorClinicResponse.from(doctorClinics.updateSettings(
                new DoctorClinicCommands.UpdateDoctorClinic(UUID.fromString(jwt.getSubject()), clinicId,
                        doctorId, request.expectedVersion(), request.consultationFee(), request.followUpFee(),
                        request.followUpValidityDays(), request.instaPayAddress(), request.autoConfirm())));
    }

    @GetMapping("/clinics/{clinicId}/doctors")
    List<DoctorClinicApiModels.DoctorClinicResponse> listClinicDoctors(
            @AuthenticationPrincipal Jwt jwt, @PathVariable UUID clinicId
    ) {
        return doctorClinics.findForClinic(clinicId, UUID.fromString(jwt.getSubject()))
                .stream().map(DoctorClinicApiModels.DoctorClinicResponse::from).toList();
    }

    @GetMapping("/doctors/me/clinics")
    List<DoctorClinicApiModels.DoctorClinicResponse> listMyClinics(@AuthenticationPrincipal Jwt jwt) {
        return doctorClinics.findForDoctorUser(UUID.fromString(jwt.getSubject()))
                .stream().map(DoctorClinicApiModels.DoctorClinicResponse::from).toList();
    }
}
