package com.example.hotelbooking.payment;

import com.example.hotelbooking.TestFixture;
import com.example.hotelbooking.entity.BookingStatus;
import com.example.hotelbooking.policy.FullRefundBeforeCheckInPolicy;
import com.example.hotelbooking.payment.mock.MockPaymentGateway;
import com.example.hotelbooking.service.BookingServiceImpl;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.util.List;
import java.util.UUID;

import static com.example.hotelbooking.TestFixture.*;
import static org.assertj.core.api.Assertions.*;

class PaymentGatewaysTest {
    @ParameterizedTest
    @ValueSource(strings = {"CARD", "UPI", "WALLET"})
    void mockMethodsSupportSuccessDeclineAndRefundFailure(String method) {
        var gateway = new MockPaymentGateway(method);
        assertThat(gateway.pay(UUID.randomUUID(), money("100"), "decline").successful()).isFalse();
        var paid = gateway.pay(UUID.randomUUID(), money("100"), "ok");
        assertThat(paid.successful()).isTrue();
        assertThat(gateway.refund(paid.reference(), money("100")).successful()).isTrue();
        var nonRefundable = gateway.pay(UUID.randomUUID(), money("100"), "refund-fail");
        assertThat(gateway.refund(nonRefundable.reference(), money("100")).successful()).isFalse();
    }

    @Test
    void newPaymentMethodRequiresNoChangeToBookingService() {
        var fixture = new TestFixture();
        var booking = fixture.book(fixture.property(1), STAY);
        var gateways = new PaymentGateways(List.of(fixture.gateway, new MockPaymentGateway("BANK")));
        var service = new BookingServiceImpl(fixture.bookings, fixture.properties, fixture.availability, fixture.lock,
                gateways, new FullRefundBeforeCheckInPolicy(), CLOCK);
        var paid = service.pay(booking.id(), " bank ", "ok");
        assertThat(paid.status()).isEqualTo(BookingStatus.CONFIRMED);
        assertThat(paid.successfulPayment().method()).isEqualTo("BANK");
    }

    @Test
    void duplicateMethodRegistrationIsRejected() {
        assertThatThrownBy(() -> new PaymentGateways(List.of(new MockPaymentGateway("CARD"),
                new MockPaymentGateway("card")))).isInstanceOf(IllegalArgumentException.class);
    }
}
