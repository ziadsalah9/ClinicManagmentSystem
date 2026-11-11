package com.clinic.backend.doctorclinic.internal.presentation;

import com.clinic.backend.clinic.ClinicReference;
import com.clinic.backend.doctor.DoctorReference;
import com.clinic.backend.doctorclinic.DoctorClinicInfo;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.util.UUID;

final class DoctorClinicApiModels {
    private DoctorClinicApiModels() { }

    record UpdateDoctorClinicRequest(
            @NotNull @DecimalMin("0.00") @Digits(integer = 10, fraction = 2) BigDecimal consultationFee,
            @NotNull @DecimalMin("0.00") @Digits(integer = 10, fraction = 2) BigDecimal followUpFee,
            @Min(1) @Max(365) int followUpValidityDays,
            @Size(max = 150) String instaPayAddress,
            @NotNull Boolean autoConfirm,
            @Min(0) Long expectedVersion
    ) { }

    record DoctorClinicResponse(
            UUID id,
            UUID clinicId,
            String clinicName,
            UUID doctorId,
            UUID doctorUserId,
            String doctorName,
            String professionalTitle,
            String doctorVerificationStatus,
            BigDecimal consultationFee,
            BigDecimal followUpFee,
            int followUpValidityDays,
            String instaPayAddress,
            boolean autoConfirm,
            String status,
            long version
    ) {
        static DoctorClinicResponse from(DoctorClinicInfo info) {
            ClinicReference clinic = info.clinic();
            DoctorReference doctor = info.doctor();
            return new DoctorClinicResponse(info.id(), clinic.id(), clinic.name(), doctor.id(),
                    doctor.userId(), doctor.fullName(), doctor.professionalTitle(), doctor.verificationStatus(),
                    info.consultationFee(), info.followUpFee(), info.followUpValidityDays(),
                    info.instaPayAddress(), info.autoConfirm(), info.status(), info.version());
        }
    }
}
