package com.example.hotelbooking.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class PaymentRequest {
    @NotBlank
    private String method;

    @NotBlank
    private String paymentToken;
}
