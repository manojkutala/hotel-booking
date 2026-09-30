package com.example.hotelbooking.dto;

import com.example.hotelbooking.entity.Owner;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class OwnerResponse {
    private UUID id;

    private String name;

    private String email;

    public static OwnerResponse from(Owner owner) {
        return new OwnerResponse(owner.id(), owner.name(), owner.email());
    }
}
