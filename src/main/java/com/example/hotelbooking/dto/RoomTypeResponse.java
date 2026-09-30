package com.example.hotelbooking.dto;

import com.example.hotelbooking.entity.RoomType;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class RoomTypeResponse {
    private UUID id;

    private String name;

    private int guestCapacity;

    private int totalRooms;

    private MoneyResponse nightlyPrice;

    public static RoomTypeResponse from(RoomType room) {
        return new RoomTypeResponse(room.id(), room.name(), room.guestCapacity(), room.totalRooms(),
                MoneyResponse.from(room.nightlyPrice()));
    }
}
