package com.example.hotelbooking.repository.inmemory;

import com.example.hotelbooking.entity.Owner;
import com.example.hotelbooking.repository.OwnerRepository;
import org.springframework.stereotype.Repository;

import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

@Repository
public class InMemoryOwnerRepository implements OwnerRepository {
    private final Map<UUID, Owner> owners = new ConcurrentHashMap<>();

    @Override
    public Owner save(Owner owner) {
        owners.put(owner.id(), owner);
        return owner;
    }

    @Override
    public Optional<Owner> findById(UUID id) {
        return Optional.ofNullable(owners.get(id));
    }
}
