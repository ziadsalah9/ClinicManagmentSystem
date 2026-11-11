package com.clinic.backend.doctorclinic;

import java.math.BigDecimal;
import java.util.UUID;

/** Minimal pricing contract for trusted consumers such as the future Billing module. */
public record DoctorClinicPricing(
        UUID doctorClinicId,
        UUID clinicId,
        UUID doctorId,
        BigDecimal consultationFee,
        BigDecimal followUpFee,
        int followUpValidityDays,
        String instaPayAddress,
        long version
) { }
