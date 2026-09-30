package com.example.hotelbooking.policy;

import com.example.hotelbooking.TestFixture;
import com.example.hotelbooking.entity.Payment;
import com.example.hotelbooking.policy.FullRefundBeforeCheckInPolicy;
import com.example.hotelbooking.exception.BusinessException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.time.LocalDate;

import static com.example.hotelbooking.TestFixture.*;
import static org.assertj.core.api.Assertions.*;

class CancellationPolicyTest {
    private final FullRefundBeforeCheckInPolicy policy = new FullRefundBeforeCheckInPolicy();

    @Test
    void refundsTheAmountPaidBeforeCheckIn() {
        var fixture = new TestFixture();
        var pending = fixture.book(fixture.property(1), STAY);
        assertThat(policy.refundAmount(pending, LocalDate.of(2030, 1, 9))).isEqualTo(money("0"));
        var confirmed = pending.recordPayment(new Payment("p1", "CARD", pending.total(),
                Payment.Status.SUCCEEDED, "paid", CLOCK.instant()));
        assertThat(policy.refundAmount(confirmed, LocalDate.of(2030, 1, 9))).isEqualTo(money("4000"));
    }

    @ParameterizedTest
    @ValueSource(strings = {"2030-01-10", "2030-01-11", "2030-01-12"})
    void rejectsCancellationOnOrAfterCheckIn(String date) {
        var fixture = new TestFixture();
        var booking = fixture.book(fixture.property(1), STAY);
        assertThatThrownBy(() -> policy.refundAmount(booking, LocalDate.parse(date)))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("before check-in");
    }
}
