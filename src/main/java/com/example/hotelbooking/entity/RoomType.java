package com.example.hotelbooking.entity;

import java.util.Objects;
import java.util.UUID;

public record RoomType(UUID id, String name, int guestCapacity, int totalRooms, Money nightlyPrice) {
    public RoomType {
        Objects.requireNonNull(id, "id");
        name = Checks.text(name, "Room type name");
        Checks.positive(guestCapacity, "Guest capacity");
        Checks.positive(totalRooms, "Room inventory");
        Objects.requireNonNull(nightlyPrice, "nightlyPrice");
        if (nightlyPrice.amount().signum() == 0) {
            throw new IllegalArgumentException("Nightly price must be positive");
        }
    }

    public boolean accommodates(int guests) {
        return guests > 0 && guests <= guestCapacity;
    }
}
