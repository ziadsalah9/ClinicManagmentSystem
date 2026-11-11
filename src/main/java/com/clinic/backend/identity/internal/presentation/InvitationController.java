package com.clinic.backend.identity.internal.presentation;

import com.clinic.backend.identity.internal.application.IdentityApplicationService;
import com.clinic.backend.identity.internal.application.IdentityCommands;
import com.clinic.backend.identity.internal.presentation.IdentityApiModels.CreateInvitationRequest;
import com.clinic.backend.identity.internal.presentation.IdentityApiModels.CreateInvitationResponse;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/clinics/{clinicId}/invitations")
public class InvitationController {
    private final IdentityApplicationService identity;

    public InvitationController(IdentityApplicationService identity) {
        this.identity = identity;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public CreateInvitationResponse create(
            @PathVariable UUID clinicId,
            @AuthenticationPrincipal Jwt jwt,
            @Valid @RequestBody CreateInvitationRequest request
    ) {
        var result = identity.createInvitation(new IdentityCommands.CreateInvitation(
                UUID.fromString(jwt.getSubject()), clinicId, request.phone(), request.role(), request.permissions()));
        return new CreateInvitationResponse(result.invitationId(), result.expiresAt(),
                "Invitation created. Delivery is handled by the configured notification provider.");
    }
}
