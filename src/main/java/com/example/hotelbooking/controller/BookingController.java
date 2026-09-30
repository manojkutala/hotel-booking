package com.example.hotelbooking.controller;

import com.example.hotelbooking.dto.BookingResponse;
import com.example.hotelbooking.dto.CreateBookingRequest;
import com.example.hotelbooking.dto.PaymentRequest;
import com.example.hotelbooking.entity.DateRange;
import com.example.hotelbooking.service.BookingService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.net.URI;
import java.util.UUID;

@RestController
@RequestMapping("/bookings")
@Validated
public class BookingController {
    private final BookingService bookingService;

    public BookingController(BookingService bookingService) {
        this.bookingService = bookingService;
    }

    @PostMapping
    public ResponseEntity<BookingResponse> createBooking(@Valid @RequestBody CreateBookingRequest request) {
        var booking = bookingService.book(request.getPropertyId(), request.getRoomTypeId(),
                new DateRange(request.getCheckIn(), request.getCheckOut()), request.getGuests(),
                request.getGuestName(), request.getGuestEmail());
        return ResponseEntity.created(URI.create("/bookings/" + booking.id())).body(BookingResponse.from(booking));
    }

    @GetMapping("/{bookingId}")
    public ResponseEntity<BookingResponse> getBooking(@PathVariable("bookingId") UUID bookingId) {
        return ResponseEntity.ok(BookingResponse.from(bookingService.getBooking(bookingId)));
    }

    @PostMapping("/{bookingId}/payments")
    public ResponseEntity<BookingResponse> payForBooking(@PathVariable("bookingId") UUID bookingId,
                                                        @Valid @RequestBody PaymentRequest request) {
        return ResponseEntity.ok(BookingResponse.from(
                bookingService.pay(bookingId, request.getMethod(), request.getPaymentToken())));
    }

    @PostMapping("/{bookingId}/cancellations")
    public ResponseEntity<BookingResponse> cancelBooking(@PathVariable("bookingId") UUID bookingId) {
        return ResponseEntity.ok(BookingResponse.from(bookingService.cancel(bookingId)));
    }
}
