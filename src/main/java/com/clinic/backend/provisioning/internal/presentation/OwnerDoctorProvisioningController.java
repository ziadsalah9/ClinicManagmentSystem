package com.clinic.backend.provisioning.internal.presentation;

import com.clinic.backend.provisioning.internal.application.ProvisioningApplicationService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/owners")
class OwnerDoctorProvisioningController {
    private final ProvisioningApplicationService provisioning;

    OwnerDoctorProvisioningController(ProvisioningApplicationService provisioning) {
        this.provisioning = provisioning;
    }

    @io.swagger.v3.oas.annotations.Hidden
    @PostMapping("/doctors")
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasRole('ADMIN') or hasRole('OWNER')")
    ProvisioningApiModels.ProvisionDoctorResponse provisionDoctor(@AuthenticationPrincipal Jwt jwt,
                                                                  @Valid @RequestBody ProvisioningApiModels.ProvisionDoctorRequest request) {
        var result = provisioning.provisionDoctor(UUID.fromString(jwt.getSubject()), request);
        return new ProvisioningApiModels.ProvisionDoctorResponse(result.user(), result.membership(), result.doctor(),
                "Doctor account, clinic membership, and doctor profile created. Doctor can log in with password/OTP.");
    }

    @PostMapping("/doctors/invite")
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasRole('ADMIN') or hasRole('OWNER')")
    ProvisioningApiModels.InviteDoctorResponse inviteDoctor(@AuthenticationPrincipal Jwt jwt,
                                                            @Valid @RequestBody ProvisioningApiModels.InviteDoctorRequest request) {
        var outcome = provisioning.inviteDoctor(UUID.fromString(jwt.getSubject()), request);
        return outcome.invited()
                ? ProvisioningApiModels.InviteDoctorResponse.invited(outcome.inviteId(), outcome.phone(), outcome.expiresAt())
                : ProvisioningApiModels.InviteDoctorResponse.addedExisting(outcome.phone(), outcome.user(), outcome.membership());
    }
}
