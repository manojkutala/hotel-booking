package com.example.hotelbooking.payment.mock;

import com.example.hotelbooking.entity.Money;
import com.example.hotelbooking.payment.PaymentGateway;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * No external calls or real payment details. Tokens "decline" and "refund-fail"
 * make the failure paths reproducible; all other nonblank tokens succeed.
 */
public class MockPaymentGateway implements PaymentGateway {
    private final String method;
    private final Map<String, Boolean> refundFailures = new ConcurrentHashMap<>();

    public MockPaymentGateway(String method) {
        this.method = method;
    }

    public String method() {
        return method;
    }

    public Result pay(UUID bookingId, Money amount, String paymentToken) {
        String reference = UUID.randomUUID().toString();
        if ("decline".equals(paymentToken)) {
            return new Result(false, reference, "Mock payment declined");
        }
        refundFailures.put(reference, "refund-fail".equals(paymentToken));
        return new Result(true, reference, "Mock payment accepted");
    }

    public Result refund(String paymentReference, Money amount) {
        Boolean fails = refundFailures.get(paymentReference);
        if (fails == null || fails) {
            return new Result(false, null, "Mock refund failed");
        }
        return new Result(true, UUID.randomUUID().toString(), "Mock refund accepted");
    }
}
