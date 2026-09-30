package com.example.hotelbooking.service;

import com.example.hotelbooking.entity.DateRange;
import com.example.hotelbooking.entity.RoomType;

public interface AvailabilityService {
    int availableRooms(RoomType roomType, DateRange stay);
}
