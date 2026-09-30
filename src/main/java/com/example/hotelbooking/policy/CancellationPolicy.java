package com.example.hotelbooking.policy;

import com.example.hotelbooking.entity.Booking;
import com.example.hotelbooking.entity.Money;

import java.time.LocalDate;

public interface CancellationPolicy {
    Money refundAmount(Booking booking, LocalDate today);
}
