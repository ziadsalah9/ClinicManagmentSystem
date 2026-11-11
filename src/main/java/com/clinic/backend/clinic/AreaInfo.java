package com.clinic.backend.clinic;

import java.util.UUID;

/** Public read-only area contract; persistence details stay inside the Clinic module. */
public record AreaInfo(UUID id, String code, String nameEn, String nameAr, String city) {
}
