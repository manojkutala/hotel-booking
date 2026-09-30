package com.example.hotelbooking.service;

import com.example.hotelbooking.TestFixture;
import com.example.hotelbooking.entity.*;
import com.example.hotelbooking.policy.FullRefundBeforeCheckInPolicy;
import com.example.hotelbooking.exception.BusinessException;
import com.example.hotelbooking.payment.PaymentGateways;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.UUID;

import static com.example.hotelbooking.TestFixture.*;
import static org.assertj.core.api.Assertions.*;

class BookingServiceImplTest {
    private TestFixture fixture;
    private Property property;

    @BeforeEach
    void setUp() {
        fixture = new TestFixture();
        property = fixture.property(1);
    }

    @Test
    void pendingBookingHoldsInventoryAndCheckoutDayIsReusable() {
        var booking = fixture.book(property, STAY);
        assertThat(booking.total()).isEqualTo(money("4000"));
        assertThat(fixture.availability.availableRooms(property.roomTypes().getFirst(), STAY)).isZero();
        assertThatThrownBy(() -> fixture.book(property, STAY))
                .isInstanceOf(BusinessException.class).hasMessageContaining("No rooms");
        var nextStay = new DateRange(STAY.checkOut(), STAY.checkOut().plusDays(1));
        assertThat(fixture.book(property, nextStay).status()).isEqualTo(BookingStatus.PENDING_PAYMENT);
    }

    @Test
    void countsOccupancyPerNightForMultipleRooms() {
        property = fixture.property(2);
        fixture.book(property, new DateRange(STAY.checkIn(), STAY.checkIn().plusDays(1)));
        fixture.book(property, new DateRange(STAY.checkIn().plusDays(1), STAY.checkOut()));
        assertThat(fixture.availability.availableRooms(property.roomTypes().getFirst(), STAY)).isEqualTo(1);
        fixture.book(property, STAY);
        assertThat(fixture.availability.availableRooms(property.roomTypes().getFirst(), STAY)).isZero();
    }

    @Test
    void soldOutNightBlocksTheWholeStay() {
        fixture.book(property, new DateRange(STAY.checkIn().plusDays(1), STAY.checkOut()));
        assertThatThrownBy(() -> fixture.book(property, STAY))
                .isInstanceOf(BusinessException.class).hasMessageContaining("No rooms");
    }

    @Test
    void validatesCapacityPastDatesAndRoomOwnership() {
        var room = property.roomTypes().getFirst();
        assertThatThrownBy(() -> fixture.bookingService.book(property.id(), room.id(), STAY, 3, "G", "g@example.com"))
                .isInstanceOf(IllegalArgumentException.class);
        var past = new DateRange(STAY.checkIn().minusYears(1), STAY.checkOut().minusYears(1));
        assertThatThrownBy(() -> fixture.book(property, past)).isInstanceOf(IllegalArgumentException.class);
        var other = fixture.property(1);
        assertThatThrownBy(() -> fixture.bookingService.book(property.id(), other.roomTypes().getFirst().id(),
                STAY, 1, "G", "g@example.com")).isInstanceOf(BusinessException.class);
        assertThatThrownBy(() -> fixture.bookingService.getBooking(UUID.randomUUID()))
                .isInstanceOf(BusinessException.class);
    }

    @Test
    void declineCanBeRetriedAndSuccessIsIdempotent() {
        var booking = fixture.book(property, STAY);
        fixture.gateway.declinePayment = true;
        var declined = fixture.bookingService.pay(booking.id(), "CARD", "token");
        assertThat(declined.status()).isEqualTo(BookingStatus.PENDING_PAYMENT);
        assertThat(declined.payments().getFirst().status()).isEqualTo(Payment.Status.FAILED);
        fixture.gateway.declinePayment = false;
        var paid = fixture.bookingService.pay(booking.id(), "card", "token");
        assertThat(paid.status()).isEqualTo(BookingStatus.CONFIRMED);
        assertThat(fixture.bookingService.pay(booking.id(), "CARD", "token")).isEqualTo(paid);
        assertThat(fixture.gateway.charges.get()).isEqualTo(2);
    }

    @Test
    void paymentUsesStoredQuoteEvenIfCataloguePriceChanges() {
        var booking = fixture.book(property, STAY);
        var room = property.roomTypes().getFirst();
        var changedRoom = new RoomType(room.id(), room.name(), room.guestCapacity(), room.totalRooms(), money("9999"));
        fixture.properties.save(new Property(property.id(), property.ownerId(), property.name(), property.city(),
                property.locality(), property.stars(), property.amenities(), List.of(changedRoom)));
        fixture.bookingService.pay(booking.id(), "CARD", "token");
        assertThat(fixture.gateway.chargedAmount).isEqualTo(money("4000"));
    }

    @Test
    void unsupportedPaymentMethodDoesNotChangeBookingOrCallGateway() {
        var booking = fixture.book(property, STAY);
        assertThatThrownBy(() -> fixture.bookingService.pay(booking.id(), "UNKNOWN", "token"))
                .isInstanceOf(IllegalArgumentException.class);
        assertThat(fixture.gateway.charges.get()).isZero();
        assertThat(fixture.bookingService.getBooking(booking.id())).isEqualTo(booking);
    }

    @Test
    void confirmedCancellationRefundsOnceAndReleasesInventory() {
        var booking = fixture.book(property, STAY);
        fixture.bookingService.pay(booking.id(), "CARD", "token");
        var cancelled = fixture.bookingService.cancel(booking.id());
        assertThat(cancelled.status()).isEqualTo(BookingStatus.CANCELLED);
        assertThat(cancelled.refund().amount()).isEqualTo(booking.total());
        assertThat(fixture.bookingService.cancel(booking.id())).isEqualTo(cancelled);
        assertThat(fixture.gateway.refunds.get()).isEqualTo(1);
        assertThat(fixture.availability.availableRooms(property.roomTypes().getFirst(), STAY)).isEqualTo(1);
        assertThatThrownBy(() -> fixture.bookingService.pay(booking.id(), "CARD", "token"))
                .isInstanceOf(BusinessException.class);
        assertThat(fixture.gateway.charges.get()).isEqualTo(1);
    }

    @Test
    void pendingCancellationNeedsNoGatewayRefund() {
        var booking = fixture.book(property, STAY);
        var cancelled = fixture.bookingService.cancel(booking.id());
        assertThat(cancelled.refund().amount()).isEqualTo(money("0"));
        assertThat(fixture.gateway.refunds.get()).isZero();
        assertThat(fixture.availability.availableRooms(property.roomTypes().getFirst(), STAY)).isEqualTo(1);
    }

    @Test
    void failedRefundPreservesConfirmedBookingAndInventoryUntilSuccessfulRetry() {
        var booking = fixture.book(property, STAY);
        fixture.bookingService.pay(booking.id(), "CARD", "token");
        fixture.gateway.declineRefund = true;
        assertThatThrownBy(() -> fixture.bookingService.cancel(booking.id()))
                .isInstanceOf(BusinessException.class);
        assertThat(fixture.bookingService.getBooking(booking.id()).status()).isEqualTo(BookingStatus.CONFIRMED);
        assertThat(fixture.availability.availableRooms(property.roomTypes().getFirst(), STAY)).isZero();
        fixture.gateway.declineRefund = false;
        assertThat(fixture.bookingService.cancel(booking.id()).status()).isEqualTo(BookingStatus.CANCELLED);
    }

    @Test
    void injectedClockControlsCancellationCutoffAndRepeatCancellationStillSucceeds() {
        var booking = fixture.book(property, STAY);
        var secondProperty = fixture.property(1);
        var alreadyCancelled = fixture.bookingService.cancel(fixture.book(secondProperty, STAY).id());
        var checkInClock = Clock.fixed(Instant.parse("2030-01-10T00:00:00Z"), ZoneOffset.UTC);
        var onCheckInDay = new BookingServiceImpl(fixture.bookings, fixture.properties, fixture.availability, fixture.lock,
                new PaymentGateways(List.of(fixture.gateway)), new FullRefundBeforeCheckInPolicy(), checkInClock);
        assertThatThrownBy(() -> onCheckInDay.cancel(booking.id())).isInstanceOf(BusinessException.class);
        assertThat(onCheckInDay.cancel(alreadyCancelled.id())).isEqualTo(alreadyCancelled);
    }

    @Test
    void anotherCancellationPolicyCanBeSuppliedWithoutChangingService() {
        var booking = fixture.book(property, STAY);
        fixture.bookingService.pay(booking.id(), "CARD", "token");
        var partialRefundService = new BookingServiceImpl(fixture.bookings, fixture.properties,
                fixture.availability, fixture.lock, new PaymentGateways(List.of(fixture.gateway)),
                (currentBooking, today) -> money("2000"), CLOCK);
        assertThat(partialRefundService.cancel(booking.id()).refund().amount()).isEqualTo(money("2000"));
    }

    @Test
    void invalidPolicyAmountIsRejectedBeforeCallingRefundGateway() {
        var booking = fixture.book(property, STAY);
        fixture.bookingService.pay(booking.id(), "CARD", "token");
        var invalidPolicyService = new BookingServiceImpl(fixture.bookings, fixture.properties,
                fixture.availability, fixture.lock, new PaymentGateways(List.of(fixture.gateway)),
                (currentBooking, today) -> money("4001"), CLOCK);
        assertThatThrownBy(() -> invalidPolicyService.cancel(booking.id()))
                .isInstanceOf(IllegalArgumentException.class);
        assertThat(fixture.gateway.refunds.get()).isZero();
        assertThat(fixture.bookingService.getBooking(booking.id()).status()).isEqualTo(BookingStatus.CONFIRMED);
    }
}
