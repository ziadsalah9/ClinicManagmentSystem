package com.clinic.backend.doctor.internal.domain;

import java.util.UUID;

public record Specialty(UUID id, String code, String nameEn, String nameAr, boolean active) { }
