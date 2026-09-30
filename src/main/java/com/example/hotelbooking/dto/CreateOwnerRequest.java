package com.example.hotelbooking.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class CreateOwnerRequest {
    @NotBlank
    private String name;

    @NotBlank
    @Email
    private String email;
}
