package com.clinic.backend.booking;

import java.time.LocalDate;
import java.time.LocalTime;

/** A deterministic session slot requested by Scheduling for one date. */
public record BookingSessionSpecification(
        LocalDate sessionDate,
        LocalTime startTime,
        LocalTime endTime,
        int capacity,
        int followUpQuota
) { }
