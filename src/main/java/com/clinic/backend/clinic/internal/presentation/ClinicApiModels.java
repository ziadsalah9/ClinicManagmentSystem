package com.clinic.backend.clinic.internal.presentation;

import com.clinic.backend.clinic.AreaInfo;
import com.clinic.backend.clinic.ClinicInfo;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;

import java.util.List;
import java.util.UUID;

final class ClinicApiModels {
    private ClinicApiModels() { }

    record CreateClinicRequest(
            @NotBlank @Size(max = 150) String name,
            @NotBlank @Size(max = 300) String address,
            @NotNull UUID areaId,
            @Size(max = 64) String timeZone,
            @NotEmpty @Size(max = 5) List<@NotBlank @Pattern(regexp = "^\\+[1-9]\\d{7,14}$") String> phones
    ) { }

    record UpdateClinicRequest(
            @NotBlank @Size(max = 150) String name,
            @NotBlank @Size(max = 300) String address,
            @NotNull UUID areaId,
            @Size(max = 64) String timeZone,
            @NotEmpty @Size(max = 5) List<@NotBlank @Pattern(regexp = "^\\+[1-9]\\d{7,14}$") String> phones,
            @NotNull @PositiveOrZero Long expectedVersion
    ) { }

    record ClinicResponse(UUID id, UUID ownerUserId, String name, String address,
                          AreaInfo area, String timeZone, String status,
                          List<String> phones, long version) {
        static ClinicResponse from(ClinicInfo info) {
            return new ClinicResponse(info.id(), info.ownerUserId(), info.name(), info.address(),
                    info.area(), info.timeZone(), info.status(), info.phones(), info.version());
        }
    }

    record AreaResponse(UUID id, String code, String nameEn, String nameAr, String city) {
        static AreaResponse from(AreaInfo info) {
            return new AreaResponse(info.id(), info.code(), info.nameEn(), info.nameAr(), info.city());
        }
    }
}
