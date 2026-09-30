package com.example.hotelbooking.config;

import com.example.hotelbooking.payment.PaymentGateway;
import com.example.hotelbooking.payment.mock.MockPaymentGateway;
import com.example.hotelbooking.policy.CancellationPolicy;
import com.example.hotelbooking.policy.FullRefundBeforeCheckInPolicy;
import com.example.hotelbooking.service.search.PropertyFilter;
import com.example.hotelbooking.service.search.StandardPropertyFilters;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Clock;
import java.time.ZoneId;
import java.util.Currency;

@Configuration
public class ApplicationConfiguration {
    @Bean
    Clock clock(@Value("${app.time-zone:UTC}") String zone) {
        return Clock.system(ZoneId.of(zone));
    }

    @Bean
    Currency currency(@Value("${app.currency:INR}") String code) {
        return Currency.getInstance(code);
    }

    @Bean
    PropertyFilter cityFilter() {
        return StandardPropertyFilters.city();
    }

    @Bean
    PropertyFilter localityFilter() {
        return StandardPropertyFilters.locality();
    }

    @Bean
    PropertyFilter starsFilter() {
        return StandardPropertyFilters.stars();
    }

    @Bean
    PropertyFilter amenitiesFilter() {
        return StandardPropertyFilters.amenities();
    }

    @Bean
    PaymentGateway cardGateway() {
        return new MockPaymentGateway("CARD");
    }

    @Bean
    PaymentGateway upiGateway() {
        return new MockPaymentGateway("UPI");
    }

    @Bean
    PaymentGateway walletGateway() {
        return new MockPaymentGateway("WALLET");
    }

    @Bean
    CancellationPolicy cancellationPolicy() {
        return new FullRefundBeforeCheckInPolicy();
    }
}
