package com.example.hotelbooking.service;

import com.example.hotelbooking.entity.*;
import com.example.hotelbooking.exception.BusinessException;
import com.example.hotelbooking.payment.PaymentGateways;
import com.example.hotelbooking.policy.CancellationPolicy;
import com.example.hotelbooking.repository.BookingRepository;
import com.example.hotelbooking.repository.PropertyRepository;
import org.springframework.stereotype.Service;

import java.time.Clock;
import java.time.LocalDate;
import java.util.UUID;

import static com.example.hotelbooking.exception.BusinessException.Code.*;

@Service
public class BookingServiceImpl implements BookingService {
    private final BookingRepository bookings;
    private final PropertyRepository properties;
    private final AvailabilityService availability;
    private final InventoryLock inventoryLock;
    private final PaymentGateways gateways;
    private final CancellationPolicy cancellationPolicy;
    private final Clock clock;

    public BookingServiceImpl(BookingRepository bookings, PropertyRepository properties,
                              AvailabilityService availability, InventoryLock inventoryLock,
                              PaymentGateways gateways, CancellationPolicy cancellationPolicy, Clock clock) {
        this.bookings = bookings;
        this.properties = properties;
        this.availability = availability;
        this.inventoryLock = inventoryLock;
        this.gateways = gateways;
        this.cancellationPolicy = cancellationPolicy;
        this.clock = clock;
    }

    @Override
    public Booking book(UUID propertyId, UUID roomTypeId, DateRange stay, int guests,
                        String guestName, String guestEmail) {
        return inventoryLock.execute(() -> {
            stay.requireNotPast(LocalDate.now(clock));
            var property = properties.findById(propertyId)
                    .orElseThrow(() -> BusinessException.notFound("Property"));
            var room = property.roomType(roomTypeId);
            if (!room.accommodates(guests)) {
                throw new IllegalArgumentException("Guest count exceeds room capacity or is not positive");
            }
            if (availability.availableRooms(room, stay) == 0) {
                throw new BusinessException(NO_AVAILABILITY, "No rooms available for the entire stay");
            }
            return bookings.save(Booking.pending(propertyId, roomTypeId, stay, guests,
                    guestName, guestEmail, room.nightlyPrice().times(stay.nights())));
        });
    }

    @Override
    public Booking getBooking(UUID id) {
        return bookings.findById(id).orElseThrow(() -> BusinessException.notFound("Booking"));
    }

    @Override
    public Booking pay(UUID bookingId, String method, String paymentToken) {
        return inventoryLock.execute(() -> {
            var booking = getBooking(bookingId);
            if (booking.status() == BookingStatus.CONFIRMED) {
                return booking;
            }
            booking.requirePendingPayment();
            booking.stay().requireNotPast(LocalDate.now(clock));
            if (paymentToken == null || paymentToken.isBlank()) {
                throw new IllegalArgumentException("A mock payment token is required");
            }
            var gateway = gateways.forMethod(method);
            var result = gateway.pay(booking.id(), booking.total(), paymentToken);
            var payment = new Payment(result.reference(), gateway.method(), booking.total(),
                    result.successful() ? Payment.Status.SUCCEEDED : Payment.Status.FAILED,
                    result.message(), clock.instant());
            return bookings.save(booking.recordPayment(payment));
        });
    }

    @Override
    public Booking cancel(UUID bookingId) {
        return inventoryLock.execute(() -> {
            var booking = getBooking(bookingId);
            if (booking.status() == BookingStatus.CANCELLED) {
                return booking;
            }
            var amount = cancellationPolicy.refundAmount(booking, LocalDate.now(clock));
            validateRefundAmount(booking, amount);
            String reference = null;
            if (amount.amount().signum() > 0) {
                var payment = booking.successfulPayment();
                var result = gateways.forMethod(payment.method()).refund(payment.reference(), amount);
                if (!result.successful()) {
                    throw new BusinessException(REFUND_FAILED, result.message());
                }
                reference = result.reference();
            }
            return bookings.save(booking.cancel(new Refund(reference, amount, clock.instant())));
        });
    }

    private void validateRefundAmount(Booking booking, Money amount) {
        var paid = booking.status() == BookingStatus.CONFIRMED
                ? booking.total() : Money.zero(booking.total().currency());
        if (!amount.currency().equals(paid.currency()) || amount.amount().compareTo(paid.amount()) > 0) {
            throw new IllegalArgumentException("Cancellation policy returned an invalid refund amount");
        }
    }
}
