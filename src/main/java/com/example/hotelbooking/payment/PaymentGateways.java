package com.example.hotelbooking.payment;

import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

@Component
public class PaymentGateways {
    private final Map<String, PaymentGateway> gateways;

    public PaymentGateways(List<PaymentGateway> implementations) {
        var registered = new HashMap<String, PaymentGateway>();
        for (var gateway : implementations) {
            if (registered.put(normalize(gateway.method()), gateway) != null) {
                throw new IllegalArgumentException("Duplicate payment method: " + gateway.method());
            }
        }
        gateways = Map.copyOf(registered);
    }

    public PaymentGateway forMethod(String method) {
        var gateway = gateways.get(normalize(method));
        if (gateway == null) {
            throw new IllegalArgumentException("Unsupported payment method: " + method);
        }
        return gateway;
    }

    private static String normalize(String method) {
        if (method == null || method.isBlank()) {
            throw new IllegalArgumentException("Payment method is required");
        }
        return method.trim().toUpperCase(Locale.ROOT);
    }
}
