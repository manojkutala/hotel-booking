package com.example.hotelbooking.entity;

import com.example.hotelbooking.exception.BusinessException;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

import static com.example.hotelbooking.exception.BusinessException.Code.INVALID_STATE;

public record Booking(UUID id, UUID propertyId, UUID roomTypeId, DateRange stay, int guests,
                      String guestName, String guestEmail, Money total, BookingStatus status,
                      List<Payment> payments, Refund refund) {
    public Booking {
        Objects.requireNonNull(id, "id");
        Objects.requireNonNull(propertyId, "propertyId");
        Objects.requireNonNull(roomTypeId, "roomTypeId");
        Objects.requireNonNull(stay, "stay");
        Checks.positive(guests, "Guests");
        guestName = Checks.text(guestName, "Guest name");
        guestEmail = Checks.text(guestEmail, "Guest email");
        Objects.requireNonNull(total, "total");
        Objects.requireNonNull(status, "status");
        payments = List.copyOf(payments);
        long successes = payments.stream().filter(p -> p.status() == Payment.Status.SUCCEEDED).count();
        if (successes > 1 || payments.stream().anyMatch(p -> !p.amount().equals(total))) {
            throw new IllegalArgumentException("Payments must match the booking total, with at most one success");
        }
        if ((status == BookingStatus.CONFIRMED && successes != 1)
                || (status == BookingStatus.PENDING_PAYMENT && successes != 0)
                || (status == BookingStatus.CANCELLED) != (refund != null)) {
            throw new IllegalArgumentException("Booking state does not match its payment/refund records");
        }
        if (refund != null && (!refund.amount().currency().equals(total.currency())
                || refund.amount().amount().compareTo(total.amount()) > 0
                || (successes == 0 && refund.amount().amount().signum() != 0))) {
            throw new IllegalArgumentException("Refund must not exceed the amount paid");
        }
    }

    public static Booking pending(UUID propertyId, UUID roomTypeId, DateRange stay, int guests,
                                  String guestName, String guestEmail, Money total) {
        return new Booking(UUID.randomUUID(), propertyId, roomTypeId, stay, guests, guestName,
                guestEmail, total, BookingStatus.PENDING_PAYMENT, List.of(), null);
    }

    public boolean holdsInventory() {
        return status != BookingStatus.CANCELLED;
    }

    public void requirePendingPayment() {
        if (status != BookingStatus.PENDING_PAYMENT) {
            throw new BusinessException(INVALID_STATE, "Only pending bookings can accept a payment");
        }
    }

    public Booking recordPayment(Payment payment) {
        requirePendingPayment();
        var attempts = new ArrayList<>(payments);
        attempts.add(payment);
        var nextStatus = payment.status() == Payment.Status.SUCCEEDED
                ? BookingStatus.CONFIRMED : BookingStatus.PENDING_PAYMENT;
        return new Booking(id, propertyId, roomTypeId, stay, guests, guestName, guestEmail,
                total, nextStatus, attempts, null);
    }

    public Payment successfulPayment() {
        return payments.stream().filter(p -> p.status() == Payment.Status.SUCCEEDED).findFirst()
                .orElseThrow(() -> new BusinessException(INVALID_STATE, "Booking has no successful payment"));
    }

    public Booking cancel(Refund refund) {
        if (status == BookingStatus.CANCELLED) {
            return this;
        }
        Objects.requireNonNull(refund, "refund");
        return new Booking(id, propertyId, roomTypeId, stay, guests, guestName, guestEmail,
                total, BookingStatus.CANCELLED, payments, refund);
    }
}
