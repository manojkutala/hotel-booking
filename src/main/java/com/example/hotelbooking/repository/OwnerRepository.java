package com.example.hotelbooking.repository;

import com.example.hotelbooking.entity.Owner;

import java.util.Optional;
import java.util.UUID;

public interface OwnerRepository {
    Owner save(Owner owner);
    Optional<Owner> findById(UUID id);
}
