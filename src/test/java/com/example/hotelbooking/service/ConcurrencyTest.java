package com.example.hotelbooking.service;

import com.example.hotelbooking.TestFixture;
import com.example.hotelbooking.entity.Booking;
import com.example.hotelbooking.entity.BookingStatus;
import com.example.hotelbooking.exception.BusinessException;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static com.example.hotelbooking.TestFixture.STAY;
import static org.assertj.core.api.Assertions.assertThat;

class ConcurrencyTest {
    @Test
    void exactlyOneConcurrentBookingWinsTheLastRoom() throws Exception {
        var fixture = new TestFixture();
        var property = fixture.property(1);
        var results = simultaneously(12, () -> {
            try {
                fixture.book(property, STAY);
                return true;
            } catch (BusinessException exception) {
                assertThat(exception.code()).isEqualTo(BusinessException.Code.NO_AVAILABILITY);
                return false;
            }
        });
        assertThat(results.stream().filter(Boolean::booleanValue).count()).isEqualTo(1);
        assertThat(fixture.bookings.findByRoomType(property.roomTypes().getFirst().id())).hasSize(1);
        assertThat(fixture.availability.availableRooms(property.roomTypes().getFirst(), STAY)).isZero();
    }

    @Test
    void simultaneousPaymentsChargeOnlyOnce() throws Exception {
        var fixture = new TestFixture();
        var booking = fixture.book(fixture.property(1), STAY);
        var results = simultaneously(8, () -> fixture.bookingService.pay(booking.id(), "CARD", "token"));
        assertThat(results).allMatch(result -> result.status() == BookingStatus.CONFIRMED);
        assertThat(results.stream().map(Booking::successfulPayment).distinct().count()).isEqualTo(1);
        assertThat(fixture.gateway.charges.get()).isEqualTo(1);
    }

    @Test
    void simultaneousCancellationsRefundOnlyOnce() throws Exception {
        var fixture = new TestFixture();
        var property = fixture.property(1);
        var booking = fixture.book(property, STAY);
        fixture.bookingService.pay(booking.id(), "CARD", "token");
        var results = simultaneously(8, () -> fixture.bookingService.cancel(booking.id()));
        assertThat(results).allMatch(result -> result.status() == BookingStatus.CANCELLED);
        assertThat(fixture.gateway.refunds.get()).isEqualTo(1);
        assertThat(fixture.availability.availableRooms(property.roomTypes().getFirst(), STAY)).isEqualTo(1);
    }

    @Test
    void paymentRacingCancellationCannotLeaveAnUnrefundedCharge() throws Exception {
        var fixture = new TestFixture();
        var property = fixture.property(1);
        var booking = fixture.book(property, STAY);
        var operation = new AtomicInteger();
        simultaneously(2, () -> {
            if (operation.getAndIncrement() == 0) {
                try {
                    return fixture.bookingService.pay(booking.id(), "CARD", "token");
                } catch (BusinessException exception) {
                    assertThat(exception.code()).isEqualTo(BusinessException.Code.INVALID_STATE);
                    return fixture.bookingService.getBooking(booking.id());
                }
            }
            return fixture.bookingService.cancel(booking.id());
        });
        assertThat(fixture.bookingService.getBooking(booking.id()).status()).isEqualTo(BookingStatus.CANCELLED);
        assertThat(fixture.gateway.charges.get()).isBetween(0, 1);
        assertThat(fixture.gateway.refunds.get()).isEqualTo(fixture.gateway.charges.get());
        assertThat(fixture.availability.availableRooms(property.roomTypes().getFirst(), STAY)).isEqualTo(1);
    }

    private <T> List<T> simultaneously(int workers, Callable<T> operation) throws Exception {
        var ready = new CountDownLatch(workers);
        var start = new CountDownLatch(1);
        var executor = Executors.newFixedThreadPool(workers);
        try {
            var futures = new ArrayList<Future<T>>();
            for (int index = 0; index < workers; index++) {
                futures.add(executor.submit(() -> {
                    ready.countDown();
                    if (!start.await(5, TimeUnit.SECONDS)) {
                        throw new IllegalStateException("Timed out waiting for the simultaneous start");
                    }
                    return operation.call();
                }));
            }
            assertThat(ready.await(5, TimeUnit.SECONDS)).isTrue();
            start.countDown();
            var results = new ArrayList<T>();
            for (var future : futures) {
                results.add(future.get(10, TimeUnit.SECONDS));
            }
            return results;
        } finally {
            start.countDown();
            executor.shutdownNow();
        }
    }
}
