package com.clinic.backend.clinic;

import java.util.UUID;

/** Minimal clinic data for trusted cross-module use; excludes owner identity and phone contacts. */
public record ClinicReference(
        UUID id,
        String name,
        String address,
        AreaInfo area,
        String timeZone,
        String status
) { }
