package com.clinic.backend.clinic.internal.presentation;

import com.clinic.backend.clinic.internal.application.ClinicApplicationService;
import com.clinic.backend.clinic.internal.application.ClinicCommands;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/clinics")
class ClinicController {
    private final ClinicApplicationService clinics;

    ClinicController(ClinicApplicationService clinics) {
        this.clinics = clinics;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    ClinicApiModels.ClinicResponse create(
            @AuthenticationPrincipal Jwt jwt,
            @RequestHeader("Idempotency-Key") String idempotencyKey,
            @Valid @RequestBody ClinicApiModels.CreateClinicRequest request
    ) {
        return ClinicApiModels.ClinicResponse.from(clinics.create(new ClinicCommands.CreateClinic(
                UUID.fromString(jwt.getSubject()), idempotencyKey, request.name(), request.address(),
                request.areaId(), request.timeZone(), request.phones())));
    }

    @GetMapping("/me")
    java.util.List<ClinicApiModels.ClinicResponse> listMine(@AuthenticationPrincipal Jwt jwt) {
        return clinics.findMyClinics(UUID.fromString(jwt.getSubject())).stream()
                .map(ClinicApiModels.ClinicResponse::from).toList();
    }

    @GetMapping("/{clinicId}")
    ClinicApiModels.ClinicResponse get(
            @AuthenticationPrincipal Jwt jwt,
            @PathVariable UUID clinicId
    ) {
        return ClinicApiModels.ClinicResponse.from(clinics.getForMember(
                UUID.fromString(jwt.getSubject()), clinicId));
    }

    @PutMapping("/{clinicId}")
    ClinicApiModels.ClinicResponse update(
            @AuthenticationPrincipal Jwt jwt,
            @PathVariable UUID clinicId,
            @Valid @RequestBody ClinicApiModels.UpdateClinicRequest request
    ) {
        return ClinicApiModels.ClinicResponse.from(clinics.update(new ClinicCommands.UpdateClinic(
                UUID.fromString(jwt.getSubject()), clinicId, request.expectedVersion(), request.name(),
                request.address(), request.areaId(), request.timeZone(), request.phones())));
    }
}
