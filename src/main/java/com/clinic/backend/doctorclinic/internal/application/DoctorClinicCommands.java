package com.clinic.backend.doctorclinic.internal.application;

import java.math.BigDecimal;
import java.util.UUID;

public final class DoctorClinicCommands {
    private DoctorClinicCommands() { }

    public record UpdateDoctorClinic(UUID actorUserId, UUID clinicId, UUID doctorId, Long expectedVersion,
                                     BigDecimal consultationFee, BigDecimal followUpFee,
                                     int followUpValidityDays, String instaPayAddress, boolean autoConfirm) { }
}
