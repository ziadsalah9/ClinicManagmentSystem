package com.clinic.backend.doctor;

import java.util.UUID;

/** Minimal doctor data for cross-module relationships; excludes phone and professional document identifiers. */
public record DoctorReference(
        UUID id,
        UUID userId,
        String fullName,
        String professionalTitle,
        String verificationStatus
) { }
