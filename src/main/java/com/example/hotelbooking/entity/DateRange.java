package com.example.hotelbooking.entity;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.Objects;

public record DateRange(LocalDate checkIn, LocalDate checkOut) {
    public DateRange {
        Objects.requireNonNull(checkIn, "checkIn");
        Objects.requireNonNull(checkOut, "checkOut");
        if (!checkOut.isAfter(checkIn)) {
            throw new IllegalArgumentException("Check-out must be after check-in");
        }
    }

    public long nights() {
        return ChronoUnit.DAYS.between(checkIn, checkOut);
    }

    public boolean contains(LocalDate night) {
        return !night.isBefore(checkIn) && night.isBefore(checkOut);
    }

    public void requireNotPast(LocalDate today) {
        if (checkIn.isBefore(today)) {
            throw new IllegalArgumentException("Check-in must not be in the past");
        }
    }
}
