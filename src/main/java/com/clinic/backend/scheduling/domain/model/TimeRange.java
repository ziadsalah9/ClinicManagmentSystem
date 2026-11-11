package com.clinic.backend.scheduling.domain.model;

import java.time.Duration;
import java.time.LocalTime;
import java.util.Objects;

public record TimeRange(
        LocalTime start, LocalTime end
) {

    public TimeRange{
        Objects.requireNonNull(start,"Start time is Required");
        Objects.requireNonNull(end,"End time Is Required");

//        if (start.isBefore(end)) {
//            throw new IllegalArgumentException("" +
//                    "Start time must be before end time");
//        }


        if (!start.isBefore(end)) {
            throw new IllegalArgumentException(
                    "Start time must be before end time"
            );
        }


    }


    public boolean overlap(TimeRange other) {
        Objects.requireNonNull(other,"Other time range is Required");
        return start.isBefore(other.end())&&other.start().isBefore(end);
    }

    public boolean contains(TimeRange other) {
        Objects.requireNonNull(other,"Other time range is Required");

            return !other.start().isBefore(start)
                    && !other.end().isAfter(end);

    }

    public long durationInMinutes() {
        return Duration.between(start, end).toMinutes();
    }

    public long durationInSeconds() {
        return Duration.between(start, end).toSeconds();
    }

}
