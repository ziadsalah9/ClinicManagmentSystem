package com.clinic.backend.doctor.internal.presentation;

import com.clinic.backend.doctor.internal.application.DoctorApplicationService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/v1/specialties")
class SpecialtyController {
    private final DoctorApplicationService doctors;

    SpecialtyController(DoctorApplicationService doctors) {
        this.doctors = doctors;
    }

    @GetMapping
    List<DoctorApiModels.SpecialtyResponse> list() {
        return doctors.getSpecialties().stream().map(DoctorApiModels.SpecialtyResponse::from).toList();
    }
}
