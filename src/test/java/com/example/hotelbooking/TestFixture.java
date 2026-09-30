package com.example.hotelbooking;

import com.example.hotelbooking.entity.*;
import com.example.hotelbooking.policy.FullRefundBeforeCheckInPolicy;
import com.example.hotelbooking.payment.PaymentGateway;
import com.example.hotelbooking.payment.PaymentGateways;
import com.example.hotelbooking.repository.inmemory.*;
import com.example.hotelbooking.service.*;
import com.example.hotelbooking.service.search.StandardPropertyFilters;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.Currency;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;

public class TestFixture {
    public static final Clock CLOCK = Clock.fixed(Instant.parse("2030-01-01T10:00:00Z"), ZoneOffset.UTC);
    public static final Currency CURRENCY = Currency.getInstance("INR");
    public static final DateRange STAY = new DateRange(LocalDate.of(2030, 1, 10), LocalDate.of(2030, 1, 12));
    public final InMemoryOwnerRepository owners = new InMemoryOwnerRepository();
    public final InMemoryPropertyRepository properties = new InMemoryPropertyRepository();
    public final InMemoryBookingRepository bookings = new InMemoryBookingRepository();
    public final InventoryLock lock = new InventoryLock();
    public final AvailabilityService availability = new AvailabilityServiceImpl(bookings, lock);
    public final RecordingGateway gateway = new RecordingGateway();
    public final PropertyService propertyService = new PropertyServiceImpl(owners, properties, availability,
            lock, StandardPropertyFilters.all(), CURRENCY, CLOCK);
    public final BookingService bookingService = new BookingServiceImpl(bookings, properties, availability,
            lock, new PaymentGateways(List.of(gateway)), new FullRefundBeforeCheckInPolicy(), CLOCK);
    public final OwnerService ownerService = new OwnerServiceImpl(owners);
    public final Owner owner = ownerService.createOwner("Hotel Group", "owner@example.com");

    public Property property(int inventory) {
        return propertyService.addProperty(owner.id(), new PropertyService.PropertyDetails(
                "Garden Hotel", "Bengaluru", "Indiranagar", 4, Set.of("wifi", "parking"),
                List.of(new PropertyService.RoomDetails("Deluxe", 2, inventory, new BigDecimal("2000")))));
    }

    public Booking book(Property property, DateRange stay) {
        return bookingService.book(property.id(), property.roomTypes().getFirst().id(), stay, 2,
                "Guest", "guest@example.com");
    }

    public static Money money(String value) {
        return new Money(new BigDecimal(value), CURRENCY);
    }

    public static class RecordingGateway implements PaymentGateway {
        public final AtomicInteger charges = new AtomicInteger();
        public final AtomicInteger refunds = new AtomicInteger();
        public volatile boolean declinePayment;
        public volatile boolean declineRefund;
        public Money chargedAmount;

        public String method() {
            return "CARD";
        }

        public Result pay(UUID bookingId, Money amount, String token) {
            charges.incrementAndGet();
            chargedAmount = amount;
            return new Result(!declinePayment, UUID.randomUUID().toString(), "Test payment");
        }

        public Result refund(String reference, Money amount) {
            refunds.incrementAndGet();
            return new Result(!declineRefund, declineRefund ? null : UUID.randomUUID().toString(), "Test refund");
        }
    }
}
