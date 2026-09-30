package com.example.hotelbooking.entity;

import java.time.Instant;
import java.util.Objects;

public record Refund(String reference, Money amount, Instant createdAt) {
    public Refund {
        Objects.requireNonNull(amount, "amount");
        Objects.requireNonNull(createdAt, "createdAt");
        if (amount.amount().signum() > 0) {
            reference = Checks.text(reference, "Refund reference");
        }
    }
}
