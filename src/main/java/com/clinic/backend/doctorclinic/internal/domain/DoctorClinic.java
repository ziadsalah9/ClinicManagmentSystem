package com.clinic.backend.doctorclinic.internal.domain;

import java.math.BigDecimal;
import java.util.UUID;

/** Pure domain model for one doctor's operational/pricing relationship with one clinic. */
public record DoctorClinic(
        UUID id,
        UUID doctorId,
        UUID clinicId,
        BigDecimal consultationFee,
        BigDecimal followUpFee,
        int followUpValidityDays,
        String instaPayAddress,
        boolean autoConfirm,
        DoctorClinicStatus status,
        long version
) {
    public static DoctorClinic create(UUID doctorId, UUID clinicId, BigDecimal consultationFee,
                                      BigDecimal followUpFee, int followUpValidityDays,
                                      String instaPayAddress, boolean autoConfirm) {
        return new DoctorClinic(UUID.randomUUID(), doctorId, clinicId, consultationFee,
                followUpFee, followUpValidityDays, instaPayAddress, autoConfirm, DoctorClinicStatus.ACTIVE, 0);
    }

    public boolean matchesSettings(BigDecimal consultationFee, BigDecimal followUpFee,
                                   int followUpValidityDays, String instaPayAddress, boolean autoConfirm) {
        return this.consultationFee.compareTo(consultationFee) == 0
                && this.followUpFee.compareTo(followUpFee) == 0
                && this.followUpValidityDays == followUpValidityDays
                && java.util.Objects.equals(this.instaPayAddress, instaPayAddress)
                && this.autoConfirm == autoConfirm;
    }

    public DoctorClinic updateSettings(BigDecimal consultationFee, BigDecimal followUpFee,
                                       int followUpValidityDays, String instaPayAddress, boolean autoConfirm) {
        return new DoctorClinic(id, doctorId, clinicId, consultationFee, followUpFee,
                followUpValidityDays, instaPayAddress, autoConfirm, status, version);
    }

}
