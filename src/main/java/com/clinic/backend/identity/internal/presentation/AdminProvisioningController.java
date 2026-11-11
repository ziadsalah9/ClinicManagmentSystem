package com.clinic.backend.identity.internal.presentation;

import com.clinic.backend.identity.internal.application.IdentityApplicationService;
import com.clinic.backend.identity.internal.application.IdentityCommands;
import com.clinic.backend.identity.internal.presentation.IdentityApiModels.ProvisionAdminRequest;
import com.clinic.backend.identity.internal.presentation.IdentityApiModels.ProvisionAdminResponse;
import com.clinic.backend.identity.internal.presentation.IdentityApiModels.ProvisionOwnerRequest;
import com.clinic.backend.identity.internal.presentation.IdentityApiModels.ProvisionOwnerResponse;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/admin")
public class AdminProvisioningController {
    private final IdentityApplicationService identity;

    public AdminProvisioningController(IdentityApplicationService identity) {
        this.identity = identity;
    }

    @PostMapping("/owners")
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasRole('ADMIN')")
    public ProvisionOwnerResponse provisionOwner(@AuthenticationPrincipal Jwt jwt,
                                                 @Valid @RequestBody ProvisionOwnerRequest request) {
        var user = identity.provisionOwner(new IdentityCommands.ProvisionOwner(
                UUID.fromString(jwt.getSubject()), request.phone(), request.fullName(), request.email(), request.password()));
        return new ProvisionOwnerResponse(user, "Owner account created. The owner can log in with password and create clinics.");
    }

    @PostMapping("/admins")
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasRole('ADMIN')")
    public ProvisionAdminResponse provisionAdmin(@AuthenticationPrincipal Jwt jwt,
                                                 @Valid @RequestBody ProvisionAdminRequest request) {
        var user = identity.provisionAdmin(new IdentityCommands.ProvisionAdmin(
                UUID.fromString(jwt.getSubject()), request.phone(), request.fullName(), request.email(), request.password()));
        return new ProvisionAdminResponse(user, "System admin account created. The admin can log in and manage platform/admin operations.");
    }
}