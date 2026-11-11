package com.clinic.backend.clinic;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

/** Public, persistence-agnostic clinic contract. */
public record ClinicInfo(
        UUID id,
        UUID ownerUserId,
        String name,
        String address,
        AreaInfo area,
        String timeZone,
        String status,
        List<String> phones,
        long version,
        Instant createdAt
) {
    public ClinicInfo {
        phones = phones == null ? List.of() : List.copyOf(phones);
    }
}
