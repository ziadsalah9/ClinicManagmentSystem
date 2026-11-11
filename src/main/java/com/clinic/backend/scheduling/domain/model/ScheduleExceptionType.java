package com.clinic.backend.scheduling.domain.model;

public enum ScheduleExceptionType {

    /**
     * The shift or all shifts are unavailable
     * during the specified date range.
     */
    DAY_OFF,

    /**
     * The normal working hours are replaced
     * with the specified hours.
     */
    CHANGED_HOURS
}
