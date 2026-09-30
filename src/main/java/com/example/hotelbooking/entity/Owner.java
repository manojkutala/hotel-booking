package com.example.hotelbooking.entity;

import java.util.Objects;
import java.util.UUID;

public record Owner(UUID id, String name, String email) {
    public Owner {
        Objects.requireNonNull(id, "id");
        name = Checks.text(name, "Owner name");
        email = Checks.text(email, "Owner email");
    }
}
