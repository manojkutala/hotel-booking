package com.example.hotelbooking.repository;

import com.example.hotelbooking.entity.Booking;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface BookingRepository {
    Booking save(Booking booking);
    Optional<Booking> findById(UUID id);
    List<Booking> findByRoomType(UUID roomTypeId);
}
