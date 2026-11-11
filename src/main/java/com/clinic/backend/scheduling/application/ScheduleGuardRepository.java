package com.clinic.backend.scheduling.application;

import java.util.UUID;

public interface ScheduleGuardRepository {
    /** Creates the guard row when absent, then locks it until the current transaction ends. */
    void lockDoctorClinic(UUID doctorClinicId);
}
