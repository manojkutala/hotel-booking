package com.example.hotelbooking.entity;

import com.example.hotelbooking.exception.BusinessException;

import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

public record Property(UUID id, UUID ownerId, String name, String city, String locality,
                       int stars, Set<String> amenities, List<RoomType> roomTypes) {
    public Property {
        Objects.requireNonNull(id, "id");
        Objects.requireNonNull(ownerId, "ownerId");
        name = Checks.text(name, "Property name");
        city = Checks.text(city, "City");
        locality = Checks.text(locality, "Locality");
        if (stars < 1 || stars > 5) {
            throw new IllegalArgumentException("Stars must be between 1 and 5");
        }
        amenities = amenities.stream()
                .map(value -> Checks.text(value, "Amenity").toLowerCase(Locale.ROOT))
                .collect(Collectors.toUnmodifiableSet());
        roomTypes = List.copyOf(roomTypes);
        if (roomTypes.isEmpty()) {
            throw new IllegalArgumentException("At least one room type is required");
        }
        if (roomTypes.stream().map(RoomType::id).distinct().count() != roomTypes.size()) {
            throw new IllegalArgumentException("Room type IDs must be unique");
        }
    }

    public RoomType roomType(UUID roomTypeId) {
        return roomTypes.stream().filter(room -> room.id().equals(roomTypeId)).findFirst()
                .orElseThrow(() -> BusinessException.notFound("Room type in this property"));
    }
}
