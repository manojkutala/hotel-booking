package com.example.hotelbooking.repository.inmemory;

import com.example.hotelbooking.entity.Booking;
import com.example.hotelbooking.repository.BookingRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

@Repository
public class InMemoryBookingRepository implements BookingRepository {
    private final Map<UUID, Booking> bookings = new ConcurrentHashMap<>();

    @Override
    public Booking save(Booking booking) {
        bookings.put(booking.id(), booking);
        return booking;
    }

    @Override
    public Optional<Booking> findById(UUID id) {
        return Optional.ofNullable(bookings.get(id));
    }

    @Override
    public List<Booking> findByRoomType(UUID roomTypeId) {
        return bookings.values().stream().filter(b -> b.roomTypeId().equals(roomTypeId)).toList();
    }
}
