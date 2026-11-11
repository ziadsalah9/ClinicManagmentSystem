package com.clinic.backend.clinic.internal.application;

import java.util.List;
import java.util.UUID;

public final class ClinicCommands {
    private ClinicCommands() { }

    public record CreateClinic(UUID ownerUserId, String idempotencyKey, String name,
                               String address, UUID areaId, String timeZone, List<String> phones) { }

    public record UpdateClinic(UUID actorUserId, UUID clinicId, long expectedVersion,
                               String name, String address, UUID areaId,
                               String timeZone, List<String> phones) { }
}
