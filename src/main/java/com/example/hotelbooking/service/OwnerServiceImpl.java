package com.example.hotelbooking.service;

import com.example.hotelbooking.entity.Owner;
import com.example.hotelbooking.repository.OwnerRepository;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Service
public class OwnerServiceImpl implements OwnerService {
    private final OwnerRepository owners;

    public OwnerServiceImpl(OwnerRepository owners) {
        this.owners = owners;
    }

    @Override
    public Owner createOwner(String name, String email) {
        return owners.save(new Owner(UUID.randomUUID(), name, email));
    }
}
