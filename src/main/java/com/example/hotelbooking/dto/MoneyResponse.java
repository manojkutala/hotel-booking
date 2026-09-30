package com.example.hotelbooking.dto;

import com.example.hotelbooking.entity.Money;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class MoneyResponse {
    private BigDecimal amount;

    private String currency;

    public static MoneyResponse from(Money money) {
        return new MoneyResponse(money.amount(), money.currency().getCurrencyCode());
    }
}
