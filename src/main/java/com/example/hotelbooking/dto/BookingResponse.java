package com.example.hotelbooking.dto;

import com.example.hotelbooking.entity.Booking;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class BookingResponse {
    private UUID id;

    private UUID propertyId;

    private UUID roomTypeId;

    private LocalDate checkIn;

    private LocalDate checkOut;

    private int guests;

    private String guestName;

    private String guestEmail;

    private MoneyResponse total;

    private String status;

    private List<PaymentResponse> payments;

    private RefundResponse refund;

    public static BookingResponse from(Booking booking) {
        return new BookingResponse(booking.id(), booking.propertyId(), booking.roomTypeId(),
                booking.stay().checkIn(), booking.stay().checkOut(), booking.guests(),
                booking.guestName(), booking.guestEmail(), MoneyResponse.from(booking.total()),
                booking.status().name(), booking.payments().stream().map(payment ->
                new PaymentResponse(payment.reference(), payment.method(), MoneyResponse.from(payment.amount()),
                        payment.status().name(), payment.message(), payment.createdAt())).toList(),
                booking.refund() == null ? null : new RefundResponse(booking.refund().reference(),
                        MoneyResponse.from(booking.refund().amount()), booking.refund().createdAt()));
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class PaymentResponse {
        private String reference;

        private String method;

        private MoneyResponse amount;

        private String status;

        private String message;

        private Instant createdAt;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class RefundResponse {
        private String reference;

        private MoneyResponse amount;

        private Instant createdAt;
    }
}
