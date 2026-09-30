package com.example.hotelbooking.payment;

import com.example.hotelbooking.entity.Money;

import java.util.UUID;

public interface PaymentGateway {
    String method();
    Result pay(UUID bookingId, Money amount, String paymentToken);
    Result refund(String paymentReference, Money amount);

    record Result(boolean successful, String reference, String message) {}
}
