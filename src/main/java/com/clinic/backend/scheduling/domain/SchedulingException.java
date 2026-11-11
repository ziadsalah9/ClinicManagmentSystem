package com.clinic.backend.scheduling.domain;

public class SchedulingException extends RuntimeException {
    private final ScheduleError error;

    public SchedulingException(ScheduleError error, String message) {
        super(message);
        this.error = error;
    }

    public ScheduleError error() {
        return error;
    }
}
