package com.example.hotelbooking.service;

import com.example.hotelbooking.entity.Booking;
import com.example.hotelbooking.entity.DateRange;

import java.util.UUID;

public interface BookingService {
    Booking book(UUID propertyId, UUID roomTypeId, DateRange stay, int guests,
                 String guestName, String guestEmail);

    Booking getBooking(UUID id);

    Booking pay(UUID bookingId, String method, String paymentToken);

    Booking cancel(UUID bookingId);
}
