package com.clinic.backend.platformadmin.internal.presentation;

import com.clinic.backend.clinic.ClinicFacade;
import com.clinic.backend.clinic.ClinicInfo;
import com.clinic.backend.clinic.ClinicVerificationEventInfo;
import com.clinic.backend.identity.IdentityFacade;
import com.clinic.backend.identity.IdentityUser;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/platform-admin")
class PlatformAdminClinicController {
    private final ClinicFacade clinics;
    private final IdentityFacade identity;

    PlatformAdminClinicController(ClinicFacade clinics, IdentityFacade identity) {
        this.clinics = clinics;
        this.identity = identity;
    }

    record OwnerSummary(UUID id, String fullName, String phone, String status) {
        static OwnerSummary from(IdentityUser user) {
            return user == null ? null : new OwnerSummary(user.id(), user.fullName(), user.phone(), user.status());
        }
    }

    record ClinicWithOwnerResponse(ClinicInfo clinic, UUID ownerId, String ownerName, String ownerPhone,
                                   OwnerSummary owner) {
        static ClinicWithOwnerResponse from(ClinicInfo clinic, IdentityUser owner) {
            return new ClinicWithOwnerResponse(clinic, clinic.ownerUserId(),
                    owner == null ? null : owner.fullName(), owner == null ? null : owner.phone(),
                    OwnerSummary.from(owner));
        }
    }

    record VerificationEventResponse(UUID id, UUID actorUserId, String actorName, String action,
                                     String previousStatus, String newStatus, String reason,
                                     Instant createdAt) {
        static VerificationEventResponse from(ClinicVerificationEventInfo event, String actorName) {
            return new VerificationEventResponse(event.id(), event.actorUserId(), actorName,
                    event.action(), event.previousStatus(), event.newStatus(), event.reason(),
                    event.createdAt());
        }
    }

    record VerificationResponse(ClinicWithOwnerResponse clinic, List<VerificationEventResponse> history) { }

    record AdminUpdateClinicRequest(
            @NotBlank @Size(max = 150) String name,
            @NotBlank @Size(max = 300) String address,
            @NotNull UUID areaId,
            @Size(max = 64) String timeZone,
            @NotEmpty @Size(max = 5) List<@NotBlank @Pattern(regexp = "^\\+[1-9]\\d{7,14}$") String> phones,
            @NotNull @PositiveOrZero Long expectedVersion
    ) {
    }

    record VerificationActionRequest(
            @NotBlank @Pattern(
                    regexp = "APPROVE|REJECT|REQUEST_INFORMATION|SUSPEND|REACTIVATE",
                    message = "action must be APPROVE, REJECT, REQUEST_INFORMATION, SUSPEND, or REACTIVATE"
            ) String action,
            @Size(max = 1000) String reason,
            @NotNull @PositiveOrZero Long expectedVersion
    ) {
    }

    @GetMapping("/clinics")
    @PreAuthorize("hasRole('ADMIN')")
    List<ClinicWithOwnerResponse> listAllClinics(
            @RequestParam(required = false) String status,
            @RequestParam(required = false) UUID areaId,
            @RequestParam(required = false) UUID ownerUserId,
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant createdFrom,
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant createdTo
    ) {
        List<ClinicInfo> all = clinics.findAllClinicsForAdmin(status, areaId, ownerUserId,
                createdFrom, createdTo);
        return withOwners(all);
    }

    @GetMapping("/clinics/{clinicId}")
    @PreAuthorize("hasRole('ADMIN')")
    ClinicWithOwnerResponse getClinic(@PathVariable UUID clinicId) {
        ClinicInfo clinic = clinics.getClinicForAdmin(clinicId);
        IdentityUser owner = clinic.ownerUserId() == null ? null : identity.findActiveUser(clinic.ownerUserId()).orElse(null);
        return ClinicWithOwnerResponse.from(clinic, owner);
    }

    @PutMapping("/clinics/{clinicId}")
    @PreAuthorize("hasRole('ADMIN')")
    ClinicWithOwnerResponse updateClinic(@AuthenticationPrincipal Jwt jwt,
                            @PathVariable UUID clinicId,
                            @Valid @RequestBody AdminUpdateClinicRequest request) {
        ClinicInfo clinic = clinics.adminUpdateClinic(UUID.fromString(jwt.getSubject()), clinicId,
                request.expectedVersion(), request.name(), request.address(), request.areaId(),
                request.timeZone(), request.phones());
        IdentityUser owner = clinic.ownerUserId() == null ? null : identity.findActiveUser(clinic.ownerUserId()).orElse(null);
        return ClinicWithOwnerResponse.from(clinic, owner);
    }

    @GetMapping("/clinics/{clinicId}/verification-history")
    @PreAuthorize("hasRole('ADMIN')")
    List<VerificationEventResponse> verificationHistory(@AuthenticationPrincipal Jwt jwt,
                                                         @PathVariable UUID clinicId) {
        return verificationHistory(UUID.fromString(jwt.getSubject()), clinicId);
    }

    @PostMapping("/clinics/{clinicId}/verification")
    @PreAuthorize("hasRole('ADMIN')")
    VerificationResponse verifyClinic(@AuthenticationPrincipal Jwt jwt,
                                      @PathVariable UUID clinicId,
                                      @Valid @RequestBody VerificationActionRequest request) {
        UUID adminId = UUID.fromString(jwt.getSubject());
        ClinicInfo clinic = clinics.transitionClinicVerification(adminId, clinicId, request.action(),
                request.expectedVersion(), request.reason());
        IdentityUser owner = clinic.ownerUserId() == null ? null : identity.findActiveUser(clinic.ownerUserId()).orElse(null);
        return new VerificationResponse(ClinicWithOwnerResponse.from(clinic, owner),
                verificationHistory(adminId, clinicId));
    }

    private List<ClinicWithOwnerResponse> withOwners(List<ClinicInfo> all) {
        if (all.isEmpty()) return List.of();
        Set<UUID> ownerIds = all.stream().map(ClinicInfo::ownerUserId)
                .filter(Objects::nonNull).collect(Collectors.toSet());
        Map<UUID, IdentityUser> owners = identity.findActiveUsers(ownerIds).stream()
                .collect(Collectors.toMap(IdentityUser::id, Function.identity()));
        return all.stream().map(c -> ClinicWithOwnerResponse.from(c, owners.get(c.ownerUserId()))).toList();
    }

    private List<VerificationEventResponse> verificationHistory(UUID adminId, UUID clinicId) {
        List<ClinicVerificationEventInfo> events = clinics.findClinicVerificationHistory(adminId, clinicId);
        if (events.isEmpty()) return List.of();
        Set<UUID> actorIds = events.stream().map(ClinicVerificationEventInfo::actorUserId)
                .filter(Objects::nonNull).collect(Collectors.toSet());
        Map<UUID, IdentityUser> actors = identity.findActiveUsers(actorIds).stream()
                .collect(Collectors.toMap(IdentityUser::id, Function.identity()));
        return events.stream()
                .map(event -> VerificationEventResponse.from(event,
                        Optional.ofNullable(actors.get(event.actorUserId()))
                                .map(IdentityUser::fullName).orElse(null)))
                .toList();
    }
}
