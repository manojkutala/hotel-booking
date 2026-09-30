package com.example.hotelbooking.service;

import com.example.hotelbooking.entity.Booking;
import com.example.hotelbooking.entity.DateRange;
import com.example.hotelbooking.entity.RoomType;
import com.example.hotelbooking.repository.BookingRepository;
import org.springframework.stereotype.Service;

@Service
public class AvailabilityServiceImpl implements AvailabilityService {
    private final BookingRepository bookings;
    private final InventoryLock inventoryLock;

    public AvailabilityServiceImpl(BookingRepository bookings, InventoryLock inventoryLock) {
        this.bookings = bookings;
        this.inventoryLock = inventoryLock;
    }

    @Override
    public int availableRooms(RoomType roomType, DateRange stay) {
        return inventoryLock.execute(() -> {
            var active = bookings.findByRoomType(roomType.id()).stream()
                    .filter(Booking::holdsInventory).toList();
            int minimumAvailable = roomType.totalRooms();
            for (var night = stay.checkIn(); night.isBefore(stay.checkOut()); night = night.plusDays(1)) {
                var currentNight = night;
                long occupied = active.stream().filter(b -> b.stay().contains(currentNight)).count();
                minimumAvailable = Math.min(minimumAvailable, roomType.totalRooms() - (int) occupied);
            }
            return Math.max(0, minimumAvailable);
        });
    }
}
