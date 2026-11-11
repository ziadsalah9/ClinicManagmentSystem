package com.clinic.backend.clinic.internal.application;

import com.clinic.backend.clinic.AreaInfo;
import com.clinic.backend.clinic.ClinicInfo;
import com.clinic.backend.clinic.ClinicReference;
import com.clinic.backend.clinic.ClinicVerificationEventInfo;
import com.clinic.backend.clinic.internal.domain.Area;
import com.clinic.backend.clinic.internal.domain.Clinic;
import com.clinic.backend.clinic.internal.domain.ClinicError;
import com.clinic.backend.clinic.internal.domain.ClinicException;
import com.clinic.backend.clinic.internal.domain.ClinicStatus;
import com.clinic.backend.clinic.internal.domain.ClinicVerificationAction;
import com.clinic.backend.clinic.internal.domain.ClinicVerificationEvent;
import com.clinic.backend.identity.IdentityFacade;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;
import java.util.Collection;
import java.util.HexFormat;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.function.Function;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

@Service
public class ClinicApplicationService {
    private static final Pattern E164 = Pattern.compile("^\\+[1-9]\\d{7,14}$");
    private static final String DEFAULT_TIME_ZONE = "Africa/Cairo";

    private final ClinicRepository clinics;
    private final IdentityFacade identity;
    private final Clock clock;

    public ClinicApplicationService(ClinicRepository clinics, IdentityFacade identity, Clock clock) {
        this.clinics = clinics;
        this.identity = identity;
        this.clock = clock;
    }

    @Transactional
    public ClinicInfo create(ClinicCommands.CreateClinic command) {
        if (identity.findActiveUser(command.ownerUserId()).isEmpty()) {
            throw error(ClinicError.USER_NOT_FOUND, "The owner account was not found or is inactive");
        }
        // A newly registered Owner has no memberships yet. Existing staff/doctor accounts
        // cannot silently promote themselves by calling this endpoint; an existing Owner
        // may create additional clinics under the same account.
        boolean alreadyOwner = identity.hasAnyActiveRole(command.ownerUserId(), "OWNER");
        Set<UUID> currentMemberships = identity.findActiveMembershipClinicIds(command.ownerUserId());
        if (!alreadyOwner && !currentMemberships.isEmpty()) {
            throw error(ClinicError.ACCESS_DENIED,
                    "Only an Owner account can create another clinic. Staff members must be invited by an Owner.");
        }

        String key = validateIdempotencyKey(command.idempotencyKey());
        String name = required(command.name(), 150, "Clinic name");
        String address = required(command.address(), 300, "Clinic address");
        if (command.areaId() == null) throw error(ClinicError.INVALID_INPUT, "Area is required");
        String timeZone = normalizeTimeZone(command.timeZone());
        List<String> phones = normalizePhones(command.phones());
        String requestHash = hashFields(name.toLowerCase(Locale.ROOT), address,
                command.areaId().toString(), timeZone, String.join(",", phones));

        ClinicRepository.CreateIdempotencyClaim claim = clinics.claimCreateRequest(
                command.ownerUserId(), key, requestHash);
        if (!claim.newClaim()) {
            Clinic existing = clinics.findById(claim.clinicId())
                    .orElseThrow(() -> error(ClinicError.IDEMPOTENCY_IN_PROGRESS,
                            "The original clinic creation request is still being processed"));
            if (!identity.hasActiveRole(command.ownerUserId(), existing.id(), "OWNER")) {
                throw error(ClinicError.ACCESS_DENIED,
                        "Only the active clinic owner can replay this creation request");
            }
            return toInfo(existing);
        }

        Area area = findActiveArea(command.areaId());
        Clinic clinic = Clinic.create(command.ownerUserId(), name, address,
                area.id(), timeZone, phones);
        try {
            clinic = clinics.save(clinic);
            // Same database transaction: a failed Owner membership insert rolls back the Clinic.
            identity.createOwnerMembership(command.ownerUserId(), clinic.id());
            clinics.completeCreateRequest(command.ownerUserId(), key, clinic.id());
            return toInfo(clinic);
        } catch (DataIntegrityViolationException exception) {
            throw error(ClinicError.CLINIC_NAME_ALREADY_EXISTS,
                    "A clinic with this name already exists");
        }
    }

    @Transactional
    public ClinicInfo update(ClinicCommands.UpdateClinic command) {
        if (command.expectedVersion() < 0) {
            throw error(ClinicError.INVALID_INPUT,
                    "expectedVersion must be zero or greater");
        }
        if (!identity.hasPermission(command.actorUserId(), command.clinicId(), "CLINIC_PROFILE_UPDATE")) {
            throw error(ClinicError.ACCESS_DENIED, "You cannot update this clinic");
        }
        Clinic existing = clinics.findById(command.clinicId())
                .orElseThrow(() -> error(ClinicError.CLINIC_NOT_FOUND, "Clinic was not found"));
        Area area = findActiveArea(command.areaId());
        String name = required(command.name(), 150, "Clinic name");
        String address = required(command.address(), 300, "Clinic address");
        String timeZone = normalizeTimeZone(command.timeZone());
        List<String> phones = normalizePhones(command.phones());
        if (existing.matchesProfile(name, address, area.id(), timeZone, phones)) {
            return toInfo(existing); // No-op update and retry after a lost response are both safe.
        }
        if (existing.version() != command.expectedVersion()) {
            throw error(ClinicError.STALE_VERSION,
                    "Clinic data changed. Reload the latest profile before updating it.");
        }
        Clinic updated = existing.updateProfile(name, address, area.id(), timeZone, phones);
        try {
            return toInfo(clinics.save(updated));
        } catch (DataIntegrityViolationException exception) {
            throw error(ClinicError.CLINIC_NAME_ALREADY_EXISTS,
                    "A clinic with this name already exists");
        }
    }

    @Transactional(readOnly = true)
    public List<ClinicInfo> findMyClinics(UUID userId) {
        if (identity.findActiveUser(userId).isEmpty()) {
            throw error(ClinicError.USER_NOT_FOUND, "The account was not found or is inactive");
        }
        Set<UUID> activeMemberships = identity.findActiveMembershipClinicIds(userId);
        if (activeMemberships.isEmpty()) return List.of();
        return toInfos(clinics.findByIds(activeMemberships));
    }

    @Transactional(readOnly = true)
    public ClinicInfo getForMember(UUID actorUserId, UUID clinicId) {
        if (!identity.isActiveMember(actorUserId, clinicId)) {
            throw error(ClinicError.ACCESS_DENIED, "You are not an active member of this clinic");
        }
        return clinics.findById(clinicId).map(this::toInfo)
                .orElseThrow(() -> error(ClinicError.CLINIC_NOT_FOUND, "Clinic was not found"));
    }

    @Transactional(readOnly = true)
    public java.util.Optional<ClinicReference> findClinicReference(UUID clinicId) {
        return clinics.findById(clinicId).map(this::toReference);
    }

    @Transactional(readOnly = true)
    public List<ClinicReference> findClinicReferences(Collection<UUID> clinicIds) {
        if (clinicIds == null || clinicIds.isEmpty()) return List.of();
        List<Clinic> found = clinics.findByIds(clinicIds);
        if (found.isEmpty()) return List.of();
        Set<UUID> areaIds = found.stream().map(Clinic::areaId).collect(Collectors.toSet());
        Map<UUID, Area> areasById = clinics.findAreasByIds(areaIds).stream()
                .collect(Collectors.toMap(Area::id, Function.identity()));
        return found.stream().filter(clinic -> areasById.containsKey(clinic.areaId()))
                .map(clinic -> toReference(clinic, areasById.get(clinic.areaId()))).toList();
    }

    @Transactional(readOnly = true)
    public List<AreaInfo> listAreas(String city) {
        if (city != null && city.trim().length() > 100) {
            throw error(ClinicError.INVALID_INPUT, "City filter must not exceed 100 characters");
        }
        return clinics.findActiveAreas(city).stream().map(ClinicApplicationService::toAreaInfo).toList();
    }

    @Transactional(readOnly = true)
    public boolean existsClinic(UUID clinicId) {
        return clinicId != null && clinics.existsById(clinicId);
    }

    @Transactional(readOnly = true)
    public boolean isClinicOwnedBy(UUID clinicId, UUID ownerUserId) {
        return clinicId != null && ownerUserId != null && clinics.existsOwnedBy(clinicId, ownerUserId);
    }

    @Transactional(readOnly = true)
    public List<ClinicInfo> findAllClinicsForAdmin() {
        return findAllClinicsForAdmin(null, null, null, null, null);
    }

    @Transactional(readOnly = true)
    public List<ClinicInfo> findAllClinicsForAdmin(String status, UUID areaId, UUID ownerUserId,
                                                   Instant createdFrom, Instant createdTo) {
        ClinicStatus statusFilter = parseStatusFilter(status);
        if (createdFrom != null && createdTo != null && createdFrom.isAfter(createdTo)) {
            throw error(ClinicError.INVALID_INPUT, "createdFrom must not be after createdTo");
        }
        return toInfos(clinics.findAll().stream()
                .filter(clinic -> statusFilter == null || clinic.status() == statusFilter)
                .filter(clinic -> areaId == null || clinic.areaId().equals(areaId))
                .filter(clinic -> ownerUserId == null || ownerUserId.equals(clinic.ownerUserId()))
                .filter(clinic -> createdFrom == null || !clinic.createdAt().isBefore(createdFrom))
                .filter(clinic -> createdTo == null || !clinic.createdAt().isAfter(createdTo))
                .toList());
    }

    @Transactional(readOnly = true)
    public ClinicInfo getClinicForAdmin(UUID clinicId) {
        if (clinicId == null) throw error(ClinicError.INVALID_INPUT, "clinicId is required");
        return clinics.findById(clinicId).map(this::toInfo)
                .orElseThrow(() -> error(ClinicError.CLINIC_NOT_FOUND, "Clinic was not found"));
    }

    @Transactional
    public ClinicInfo transitionClinicVerification(UUID actorUserId, UUID clinicId, String actionName,
                                                   Long expectedVersion, String reason) {
        requireSystemAdmin(actorUserId);
        if (clinicId == null) throw error(ClinicError.INVALID_INPUT, "clinicId is required");
        if (expectedVersion == null || expectedVersion < 0) {
            throw error(ClinicError.INVALID_INPUT, "expectedVersion must be zero or greater");
        }
        ClinicVerificationAction action = parseVerificationAction(actionName);
        String normalizedReason = normalizeReason(reason);
        Clinic existing = clinics.findById(clinicId)
                .orElseThrow(() -> error(ClinicError.CLINIC_NOT_FOUND, "Clinic was not found"));
        if (existing.version() != expectedVersion) {
            throw error(ClinicError.STALE_VERSION,
                    "Clinic data changed. Reload the latest verification state before acting.");
        }

        Clinic updated = existing.applyVerificationAction(action, normalizedReason);
        if (action.changesStatus()) {
            updated = clinics.save(updated);
        }
        ClinicVerificationEvent event = ClinicVerificationEvent.create(clinicId, actorUserId,
                action.name(), existing.status().name(), updated.status().name(),
                normalizedReason, clock.instant());
        clinics.saveVerificationEvent(event);
        return toInfo(updated);
    }

    @Transactional(readOnly = true)
    public List<ClinicVerificationEventInfo> findVerificationHistory(UUID actorUserId, UUID clinicId) {
        requireSystemAdmin(actorUserId);
        if (clinicId == null) throw error(ClinicError.INVALID_INPUT, "clinicId is required");
        if (!clinics.existsById(clinicId)) {
            throw error(ClinicError.CLINIC_NOT_FOUND, "Clinic was not found");
        }
        return clinics.findVerificationEvents(clinicId).stream()
                .map(ClinicApplicationService::toVerificationInfo)
                .toList();
    }

    @Transactional
    public ClinicInfo adminUpdateClinic(ClinicCommands.UpdateClinic command) {
        if (command.actorUserId() == null || !identity.isSystemAdmin(command.actorUserId())) {
            throw error(ClinicError.ACCESS_DENIED, "Only a system admin can update any clinic");
        }
        if (command.expectedVersion() < 0) {
            throw error(ClinicError.INVALID_INPUT, "expectedVersion must be zero or greater");
        }
        Clinic existing = clinics.findById(command.clinicId())
                .orElseThrow(() -> error(ClinicError.CLINIC_NOT_FOUND, "Clinic was not found"));
        Area area = findActiveArea(command.areaId());
        String name = required(command.name(), 150, "Clinic name");
        String address = required(command.address(), 300, "Clinic address");
        String timeZone = normalizeTimeZone(command.timeZone());
        List<String> phones = normalizePhones(command.phones());
        if (existing.matchesProfile(name, address, area.id(), timeZone, phones)) {
            return toInfo(existing);
        }
        if (existing.version() != command.expectedVersion()) {
            throw error(ClinicError.STALE_VERSION,
                    "Clinic data changed. Reload the latest profile before updating it.");
        }
        Clinic updated = existing.updateProfile(name, address, area.id(), timeZone, phones);
        try {
            return toInfo(clinics.save(updated));
        } catch (DataIntegrityViolationException exception) {
            throw error(ClinicError.CLINIC_NAME_ALREADY_EXISTS, "A clinic with this name already exists");
        }
    }

    private Area findActiveArea(UUID areaId) {
        if (areaId == null) throw error(ClinicError.INVALID_INPUT, "Area is required");
        return clinics.findActiveAreaById(areaId)
                .orElseThrow(() -> error(ClinicError.AREA_NOT_FOUND, "Selected area does not exist or is inactive"));
    }

    private List<ClinicInfo> toInfos(List<Clinic> source) {
        if (source.isEmpty()) return List.of();
        Set<UUID> areaIds = source.stream().map(Clinic::areaId).collect(Collectors.toSet());
        Map<UUID, Area> areasById = clinics.findAreasByIds(areaIds).stream()
                .collect(Collectors.toMap(Area::id, Function.identity()));
        return source.stream().map(clinic -> {
            Area area = areasById.get(clinic.areaId());
            if (area == null) throw new IllegalStateException("Clinic references a missing area: " + clinic.areaId());
            return toInfo(clinic, area);
        }).toList();
    }

    private ClinicInfo toInfo(Clinic clinic) {
        Area area = clinics.findAreasByIds(Set.of(clinic.areaId())).stream().findFirst()
                .orElseThrow(() -> new IllegalStateException("Clinic references a missing area: " + clinic.areaId()));
        return toInfo(clinic, area);
    }

    private static ClinicInfo toInfo(Clinic clinic, Area area) {
        return new ClinicInfo(clinic.id(), clinic.ownerUserId(), clinic.name(), clinic.address(),
                toAreaInfo(area), clinic.timeZone(), clinic.status().name(), clinic.phones(),
                clinic.version(), clinic.createdAt());
    }

    private static ClinicVerificationEventInfo toVerificationInfo(ClinicVerificationEvent event) {
        return new ClinicVerificationEventInfo(event.id(), event.clinicId(), event.actorUserId(),
                event.action(), event.previousStatus(), event.newStatus(), event.reason(), event.createdAt());
    }

    private void requireSystemAdmin(UUID actorUserId) {
        if (actorUserId == null || !identity.isSystemAdmin(actorUserId)) {
            throw error(ClinicError.ACCESS_DENIED, "Only a platform administrator can perform this action");
        }
    }

    private static ClinicStatus parseStatusFilter(String value) {
        if (value == null || value.isBlank()) return null;
        try {
            return ClinicStatus.valueOf(value.trim().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException exception) {
            throw error(ClinicError.INVALID_INPUT, "Unknown clinic status filter");
        }
    }

    private static ClinicVerificationAction parseVerificationAction(String value) {
        if (value == null || value.isBlank()) {
            throw error(ClinicError.INVALID_INPUT, "Verification action is required");
        }
        try {
            return ClinicVerificationAction.valueOf(value.trim().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException exception) {
            throw error(ClinicError.INVALID_INPUT, "Unsupported verification action");
        }
    }

    private static String normalizeReason(String value) {
        if (value == null || value.isBlank()) return null;
        String reason = value.trim();
        if (reason.length() > 1000) {
            throw error(ClinicError.INVALID_INPUT, "Verification reason must not exceed 1000 characters");
        }
        return reason;
    }

    private static ClinicReference toReference(Clinic clinic, Area area) {
        return new ClinicReference(clinic.id(), clinic.name(), clinic.address(), toAreaInfo(area),
                clinic.timeZone(), clinic.status().name());
    }

    private ClinicReference toReference(Clinic clinic) {
        Area area = clinics.findAreasByIds(Set.of(clinic.areaId())).stream().findFirst()
                .orElseThrow(() -> new IllegalStateException("Clinic references a missing area: " + clinic.areaId()));
        return toReference(clinic, area);
    }

    private static AreaInfo toAreaInfo(Area area) {
        return new AreaInfo(area.id(), area.code(), area.nameEn(), area.nameAr(), area.city());
    }

    private static String required(String value, int max, String label) {
        if (value == null || value.isBlank() || value.trim().length() > max) {
            throw error(ClinicError.INVALID_INPUT,
                    label + " is required and must not exceed " + max + " characters");
        }
        return value.trim();
    }

    private static String normalizeTimeZone(String value) {
        String zone = value == null || value.isBlank() ? DEFAULT_TIME_ZONE : value.trim();
        try {
            return ZoneId.of(zone).getId();
        } catch (RuntimeException exception) {
            throw error(ClinicError.INVALID_INPUT, "Invalid IANA time zone");
        }
    }

    private static List<String> normalizePhones(List<String> input) {
        if (input == null || input.isEmpty() || input.size() > 5) {
            throw error(ClinicError.INVALID_INPUT, "Provide between 1 and 5 clinic phone numbers");
        }
        LinkedHashSet<String> phones = new LinkedHashSet<>();
        for (String phone : input) {
            if (phone == null || !E164.matcher(phone.trim()).matches()) {
                throw error(ClinicError.INVALID_INPUT, "Clinic phone numbers must use E.164 format");
            }
            phones.add(phone.trim());
        }
        if (phones.size() != input.size()) {
            throw error(ClinicError.INVALID_INPUT, "Duplicate clinic phone numbers are not allowed");
        }
        return phones.stream().sorted().toList();
    }

    private static String validateIdempotencyKey(String key) {
        if (key == null || key.isBlank() || key.length() > 100) {
            throw error(ClinicError.INVALID_INPUT,
                    "Idempotency-Key header is required and must not exceed 100 characters");
        }
        return key.trim();
    }

    private static String hashFields(String... fields) {
        StringBuilder canonical = new StringBuilder();
        for (String field : fields) {
            String value = field == null ? "" : field;
            canonical.append(value.length()).append(':').append(value);
        }
        try {
            return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256")
                    .digest(canonical.toString().getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException("SHA-256 is not available", exception);
        }
    }

    private static ClinicException error(ClinicError error, String message) {
        return new ClinicException(error, message);
    }
}
