package com.example.hotelbooking.policy;

import com.example.hotelbooking.entity.Booking;
import com.example.hotelbooking.entity.BookingStatus;
import com.example.hotelbooking.entity.Money;
import com.example.hotelbooking.exception.BusinessException;

import java.time.LocalDate;

import static com.example.hotelbooking.exception.BusinessException.Code.CANCELLATION_NOT_ALLOWED;

public class FullRefundBeforeCheckInPolicy implements CancellationPolicy {
    @Override
    public Money refundAmount(Booking booking, LocalDate today) {
        if (!today.isBefore(booking.stay().checkIn())) {
            throw new BusinessException(CANCELLATION_NOT_ALLOWED, "Cancellation is only allowed before check-in");
        }
        return booking.status() == BookingStatus.CONFIRMED
                ? booking.total() : Money.zero(booking.total().currency());
    }
}
