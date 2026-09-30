package com.example.hotelbooking.entity;

import com.example.hotelbooking.entity.*;
import com.example.hotelbooking.exception.BusinessException;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.UUID;

import static com.example.hotelbooking.TestFixture.*;
import static org.assertj.core.api.Assertions.*;

class BookingTest {
    private Booking pending() {
        return Booking.pending(UUID.randomUUID(), UUID.randomUUID(), STAY, 2,
                "Guest", "guest@example.com", money("4000"));
    }

    private Payment payment(Payment.Status status, String amount) {
        return new Payment("payment-1", "CARD", money(amount), status, "Test", CLOCK.instant());
    }

    @Test
    void successfulPaymentConfirmsBookingAndCannotBeRecordedAgain() {
        var pending = pending();
        var confirmed = pending.recordPayment(payment(Payment.Status.SUCCEEDED, "4000"));
        assertThat(confirmed.status()).isEqualTo(BookingStatus.CONFIRMED);
        assertThat(pending.status()).isEqualTo(BookingStatus.PENDING_PAYMENT);
        assertThatThrownBy(() -> confirmed.recordPayment(payment(Payment.Status.SUCCEEDED, "4000")))
                .isInstanceOf(BusinessException.class);
    }

    @Test
    void failedPaymentKeepsInventoryHeldAndAllowsAnotherAttempt() {
        var failed = pending().recordPayment(payment(Payment.Status.FAILED, "4000"));
        assertThat(failed.status()).isEqualTo(BookingStatus.PENDING_PAYMENT);
        assertThat(failed.holdsInventory()).isTrue();
        assertThat(failed.recordPayment(payment(Payment.Status.SUCCEEDED, "4000")).status())
                .isEqualTo(BookingStatus.CONFIRMED);
    }

    @Test
    void cancelledBookingRejectsPaymentAndCancellationIsRepeatable() {
        var refund = new Refund(null, money("0"), CLOCK.instant());
        var cancelled = pending().cancel(refund);
        assertThat(cancelled.holdsInventory()).isFalse();
        assertThat(cancelled.cancel(refund)).isSameAs(cancelled);
        assertThatThrownBy(() -> cancelled.recordPayment(payment(Payment.Status.SUCCEEDED, "4000")))
                .isInstanceOf(BusinessException.class);
    }

    @Test
    void paymentAmountAndRefundAmountMustMatchTheBooking() {
        assertThatThrownBy(() -> pending().recordPayment(payment(Payment.Status.SUCCEEDED, "1")))
                .isInstanceOf(IllegalArgumentException.class);
        var confirmed = pending().recordPayment(payment(Payment.Status.SUCCEEDED, "4000"));
        assertThatThrownBy(() -> confirmed.cancel(new Refund("refund", money("4001"), CLOCK.instant())))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> pending().cancel(new Refund("refund", money("1"), CLOCK.instant())))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void datesAreHalfOpenAndRequireAtLeastOneNight() {
        assertThat(STAY.nights()).isEqualTo(2);
        assertThat(STAY.contains(STAY.checkIn())).isTrue();
        assertThat(STAY.contains(STAY.checkOut())).isFalse();
        assertThatThrownBy(() -> new DateRange(STAY.checkIn(), STAY.checkIn()))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new DateRange(STAY.checkOut(), STAY.checkIn()))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> STAY.requireNotPast(LocalDate.of(2030, 1, 11)))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void moneyUsesExactDecimalArithmetic() {
        assertThat(money("19.99").times(3)).isEqualTo(money("59.97"));
        assertThat(money("20")).isEqualTo(money("20.00"));
        assertThatThrownBy(() -> money("-1")).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> money("1.001")).isInstanceOf(IllegalArgumentException.class);
    }
}
