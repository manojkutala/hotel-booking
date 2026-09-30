package com.example.hotelbooking.dto;

import com.example.hotelbooking.service.PropertyService;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.Set;
import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class PropertySearchResponse {
    private UUID id;

    private String name;

    private String city;

    private String locality;

    private int stars;

    private Set<String> amenities;

    private List<AvailableRoomResponse> roomTypes;

    public static PropertySearchResponse from(PropertyService.SearchResult result) {
        var property = result.property();
        return new PropertySearchResponse(property.id(), property.name(), property.city(),
                property.locality(), property.stars(), property.amenities(), result.rooms().stream()
                .map(room -> new AvailableRoomResponse(RoomTypeResponse.from(room.roomType()), room.availableRooms()))
                .toList());
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class AvailableRoomResponse {
        private RoomTypeResponse roomType;

        private int availableRooms;
    }
}
