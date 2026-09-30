package com.example.hotelbooking.dto;

import jakarta.validation.constraints.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class CreateBookingRequest {
    @NotNull
    private UUID propertyId;

    @NotNull
    private UUID roomTypeId;

    @NotNull
    private LocalDate checkIn;

    @NotNull
    private LocalDate checkOut;

    @Positive
    private int guests;

    @NotBlank
    private String guestName;

    @NotBlank
    @Email
    private String guestEmail;
}
