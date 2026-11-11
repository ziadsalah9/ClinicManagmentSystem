package com.clinic.backend.provisioning.internal.presentation;

import com.clinic.backend.provisioning.internal.application.ProvisioningApplicationService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/auth")
class DoctorOnboardingClaimController {
    private final ProvisioningApplicationService provisioning;

    DoctorOnboardingClaimController(ProvisioningApplicationService provisioning) {
        this.provisioning = provisioning;
    }

    @PostMapping("/doctors/claim")
    @ResponseStatus(HttpStatus.CREATED)
    ProvisioningApiModels.ClaimDoctorResponse claim(@Valid @RequestBody ProvisioningApiModels.ClaimDoctorRequest request) {
        var result = provisioning.claimDoctor(request);
        var t = result.tokens();
        return new ProvisioningApiModels.ClaimDoctorResponse(result.user(), result.membership(),
                new ProvisioningApiModels.TokenResponse(t.accessToken(), "Bearer", t.accessTokenExpiresAt(), t.refreshToken(), t.refreshTokenExpiresAt(), t.user()),
                "Doctor account claimed. Complete your doctor profile to finish onboarding.");
    }
}