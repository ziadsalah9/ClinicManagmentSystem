package com.clinic.backend.scheduling.domain.service;



import com.clinic.backend.scheduling.domain.model.ScheduleConflict;
import com.clinic.backend.scheduling.domain.model.ScheduleShift;
import com.clinic.backend.scheduling.domain.model.TimeRange;
import com.clinic.backend.scheduling.domain.model.WeeklyShiftRule;

import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;


public final class ScheduleConflictDetector {

    private ScheduleConflictDetector() {
        // Stateless domain service.
    }


    public static List<ScheduleConflict> detectConflicts(
            ScheduleShift candidate,
            List<ScheduleShift> existingShifts
    ) {

        Objects.requireNonNull(
                candidate,
                "Candidate shift is required"
        );

        Objects.requireNonNull(
                existingShifts,
                "Existing shifts list is required"
        );

        if (!candidate.active()) {
            return List.of();
        }

        List<ScheduleConflict> conflicts = new ArrayList<>();

        for (ScheduleShift existingShift : existingShifts) {

            Objects.requireNonNull(
                    existingShift,
                    "Existing shift cannot be null"
            );

            if (!candidate.doctorClinicId().equals(
                    existingShift.doctorClinicId()
            )) {
                throw new IllegalArgumentException(
                        "All shifts must belong to the same DoctorClinic"
                );
            }

            if (!existingShift.active()) {
                continue;
            }

            if (candidate.id().equals(existingShift.id())) {
                continue;
            }

            detectShiftConflicts(
                    candidate,
                    existingShift,
                    conflicts
            );
        }

        return List.copyOf(conflicts);
    }

    private static void detectShiftConflicts(
            ScheduleShift firstShift,
            ScheduleShift secondShift,
            List<ScheduleConflict> conflicts
    ) {

        for (WeeklyShiftRule firstRule : firstShift.weeklyRules()) {

            for (WeeklyShiftRule secondRule : secondShift.weeklyRules()) {

                // Different weekdays cannot conflict.
                if (firstRule.dayOfWeek() != secondRule.dayOfWeek()) {
                    continue;
                }

                TimeRange firstHours = firstRule.workingHours();
                TimeRange secondHours = secondRule.workingHours();

                if (!firstHours.overlap(secondHours)) {
                    continue;
                }

                TimeRange overlappingPeriod = calculateOverlap(
                        firstHours,
                        secondHours
                );

                conflicts.add(
                        new ScheduleConflict(
                                firstShift.id(),
                                secondShift.id(),
                                firstRule.dayOfWeek(),
                                overlappingPeriod
                        )
                );
            }
        }
    }

    private static TimeRange calculateOverlap(
            TimeRange first,
            TimeRange second
    ) {

        LocalTime overlapStart = first.start().isAfter(second.start())
                ? first.start()
                : second.start();

        LocalTime overlapEnd = first.end().isBefore(second.end())
                ? first.end()
                : second.end();

        return new TimeRange(
                overlapStart,
                overlapEnd
        );
    }
}