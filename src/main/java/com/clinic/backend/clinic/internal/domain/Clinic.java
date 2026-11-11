package com.clinic.backend.clinic.internal.domain;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

/** Pure domain model. It has no Spring or JPA dependencies. */
public record Clinic(
        UUID id,
        UUID ownerUserId,
        UUID areaId,
        String name,
        String address,
        String timeZone,
        ClinicStatus status,
        List<String> phones,
        long version,
        Instant createdAt
) {
    public Clinic {
        phones = phones == null ? List.of() : List.copyOf(phones);
    }

    public static Clinic create(UUID ownerUserId, String name, String address, UUID areaId,
                                String timeZone, List<String> phones) {
        return new Clinic(UUID.randomUUID(), ownerUserId, areaId, name, address,
                timeZone, ClinicStatus.PENDING_VERIFICATION, phones, 0, null);
    }

    public boolean matchesProfile(String name, String address, UUID areaId,
                                  String timeZone, List<String> phones) {
        return this.name.equals(name) && this.address.equals(address)
                && this.areaId.equals(areaId) && this.timeZone.equals(timeZone)
                && this.phones.equals(phones);
    }

    public Clinic updateProfile(String name, String address, UUID areaId,
                                String timeZone, List<String> phones) {
        return new Clinic(id, ownerUserId, areaId, name, address, timeZone,
                status, phones, version, createdAt);
    }

    public Clinic applyVerificationAction(ClinicVerificationAction action, String reason) {
        if (!action.allowedFrom().contains(status)) {
            throw new ClinicException(ClinicError.INVALID_STATUS_TRANSITION,
                    "Clinic status " + status + " cannot transition using " + action.name());
        }
        if (action.requiresReason() && (reason == null || reason.isBlank())) {
            throw new ClinicException(ClinicError.VERIFICATION_REASON_REQUIRED,
                    "A reason is required for " + action.name());
        }
        ClinicStatus target = action.changesStatus() ? action.targetStatus() : status;
        return new Clinic(id, ownerUserId, areaId, name, address, timeZone,
                target, phones, version, createdAt);
    }
}
