package com.example.hotelbooking.entity;

final class Checks {
    private Checks() {}

    static String text(String value, String field) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(field + " must not be blank");
        }
        return value.trim();
    }

    static void positive(int value, String field) {
        if (value < 1) {
            throw new IllegalArgumentException(field + " must be positive");
        }
    }
}
