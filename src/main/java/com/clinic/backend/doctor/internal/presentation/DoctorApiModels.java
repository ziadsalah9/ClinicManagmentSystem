package com.clinic.backend.doctor.internal.presentation;

import com.clinic.backend.doctor.DoctorInfo;
import com.clinic.backend.doctor.SpecialtyInfo;
import com.clinic.backend.doctor.internal.domain.DoctorGender;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.Set;
import java.util.UUID;

final class DoctorApiModels {
    private DoctorApiModels() { }

    record CreateDoctorProfileRequest(
            @NotBlank @Size(max = 120) String professionalTitle,
            @Size(max = 2000) String bio,
            @Min(0) @Max(80) int yearsExperience,
            @Size(max = 50) String syndicateNumber,
            @NotNull DoctorGender gender,
            @NotEmpty @Size(max = 8) Set<@NotNull UUID> specialtyIds
    ) { }

    record UpdateDoctorProfileRequest(
            @NotBlank @Size(max = 120) String professionalTitle,
            @Size(max = 2000) String bio,
            @Min(0) @Max(80) int yearsExperience,
            @Size(max = 50) String syndicateNumber,
            @NotNull DoctorGender gender,
            @NotEmpty @Size(max = 8) Set<@NotNull UUID> specialtyIds,
            @NotNull @Min(0) Long expectedVersion
    ) { }

    record DoctorResponse(UUID id, UUID userId, String fullName, String phone,
                          String professionalTitle, String bio, int yearsExperience,
                          String syndicateNumber, String gender, String verificationStatus,
                          Set<UUID> specialtyIds, long version) {
        static DoctorResponse from(DoctorInfo info) {
            return new DoctorResponse(info.id(), info.userId(), info.fullName(), info.phone(),
                    info.professionalTitle(), info.bio(), info.yearsExperience(), info.syndicateNumber(),
                    info.gender(), info.verificationStatus(), info.specialtyIds(), info.version());
        }
    }

    record SpecialtyResponse(UUID id, String code, String nameEn, String nameAr) {
        static SpecialtyResponse from(SpecialtyInfo info) {
            return new SpecialtyResponse(info.id(), info.code(), info.nameEn(), info.nameAr());
        }
    }
}
