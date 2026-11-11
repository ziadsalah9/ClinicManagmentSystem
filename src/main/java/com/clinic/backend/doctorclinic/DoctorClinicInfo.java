package com.clinic.backend.doctorclinic;

import com.clinic.backend.clinic.ClinicReference;
import com.clinic.backend.doctor.DoctorReference;

import java.math.BigDecimal;
import java.util.UUID;

/** Public contract for a doctor's configuration inside one clinic. */
public record DoctorClinicInfo(
        UUID id,
        ClinicReference clinic,
        DoctorReference doctor,
        BigDecimal consultationFee,
        BigDecimal followUpFee,
        int followUpValidityDays,
        String instaPayAddress,
        boolean autoConfirm,
        String status,
        long version
) { }
