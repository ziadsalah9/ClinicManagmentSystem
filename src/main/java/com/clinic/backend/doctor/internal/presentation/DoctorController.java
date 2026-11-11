package com.clinic.backend.doctor.internal.presentation;

import com.clinic.backend.doctor.internal.application.DoctorApplicationService;
import com.clinic.backend.doctor.internal.application.DoctorCommands;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/doctors")
class DoctorController {
    private final DoctorApplicationService doctors;

    DoctorController(DoctorApplicationService doctors) {
        this.doctors = doctors;
    }

    @GetMapping("/me")
    DoctorApiModels.DoctorResponse getMine(@AuthenticationPrincipal Jwt jwt) {
        return DoctorApiModels.DoctorResponse.from(doctors.getMyProfile(UUID.fromString(jwt.getSubject())));
    }

    @PostMapping("/me")
    @ResponseStatus(HttpStatus.CREATED)
    DoctorApiModels.DoctorResponse createMine(@AuthenticationPrincipal Jwt jwt,
                                               @Valid @RequestBody DoctorApiModels.CreateDoctorProfileRequest request) {
        return DoctorApiModels.DoctorResponse.from(doctors.createMyProfile(new DoctorCommands.SaveDoctorProfile(
                UUID.fromString(jwt.getSubject()), null, request.professionalTitle(), request.bio(),
                request.yearsExperience(), request.syndicateNumber(), request.gender(), request.specialtyIds())));
    }

    @PutMapping("/me")
    DoctorApiModels.DoctorResponse updateMine(@AuthenticationPrincipal Jwt jwt,
                                               @Valid @RequestBody DoctorApiModels.UpdateDoctorProfileRequest request) {
        return DoctorApiModels.DoctorResponse.from(doctors.updateMyProfile(new DoctorCommands.SaveDoctorProfile(
                UUID.fromString(jwt.getSubject()), request.expectedVersion(), request.professionalTitle(), request.bio(),
                request.yearsExperience(), request.syndicateNumber(), request.gender(), request.specialtyIds())));
    }
}
