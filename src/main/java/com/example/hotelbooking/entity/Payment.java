package com.example.hotelbooking.entity;

import java.time.Instant;
import java.util.Objects;

public record Payment(String reference, String method, Money amount, Status status,
                      String message, Instant createdAt) {
    public enum Status { SUCCEEDED, FAILED }

    public Payment {
        reference = Checks.text(reference, "Payment reference");
        method = Checks.text(method, "Payment method");
        Objects.requireNonNull(amount, "amount");
        Objects.requireNonNull(status, "status");
        Objects.requireNonNull(createdAt, "createdAt");
    }
}
