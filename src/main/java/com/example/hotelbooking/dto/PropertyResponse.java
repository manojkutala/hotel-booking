package com.example.hotelbooking.dto;

import com.example.hotelbooking.entity.Property;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.Set;
import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class PropertyResponse {
    private UUID id;

    private UUID ownerId;

    private String name;

    private String city;

    private String locality;

    private int stars;

    private Set<String> amenities;

    private List<RoomTypeResponse> roomTypes;

    public static PropertyResponse from(Property property) {
        return new PropertyResponse(property.id(), property.ownerId(), property.name(), property.city(),
                property.locality(), property.stars(), property.amenities(),
                property.roomTypes().stream().map(RoomTypeResponse::from).toList());
    }
}
