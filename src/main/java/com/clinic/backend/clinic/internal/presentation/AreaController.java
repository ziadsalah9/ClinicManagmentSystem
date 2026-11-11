package com.clinic.backend.clinic.internal.presentation;

import com.clinic.backend.clinic.internal.application.ClinicApplicationService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/v1/areas")
class AreaController {
    private final ClinicApplicationService clinics;

    AreaController(ClinicApplicationService clinics) {
        this.clinics = clinics;
    }

    @GetMapping
    List<ClinicApiModels.AreaResponse> list(@RequestParam(required = false) String city) {
        return clinics.listAreas(city).stream().map(ClinicApiModels.AreaResponse::from).toList();
    }
}
