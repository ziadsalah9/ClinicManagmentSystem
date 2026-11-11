package com.clinic.backend.clinic.internal.domain;

import java.util.UUID;

/** A curated, selectable location in the clinic directory. */
public record Area(UUID id, String code, String nameEn, String nameAr, String city, boolean active) {
}
