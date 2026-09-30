package com.example.hotelbooking.repository;

import com.example.hotelbooking.entity.Property;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface PropertyRepository {
    Property save(Property property);
    Optional<Property> findById(UUID id);
    List<Property> findAll();
}
