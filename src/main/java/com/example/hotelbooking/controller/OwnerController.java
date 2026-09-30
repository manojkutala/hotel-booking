package com.example.hotelbooking.controller;

import com.example.hotelbooking.dto.CreateOwnerRequest;
import com.example.hotelbooking.dto.CreatePropertyRequest;
import com.example.hotelbooking.dto.OwnerResponse;
import com.example.hotelbooking.dto.PropertyResponse;
import com.example.hotelbooking.service.OwnerService;
import com.example.hotelbooking.service.PropertyService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.net.URI;
import java.util.UUID;

@RestController
@RequestMapping("/owners")
@Validated
public class OwnerController {
    private final OwnerService ownerService;
    private final PropertyService propertyService;

    public OwnerController(OwnerService ownerService, PropertyService propertyService) {
        this.ownerService = ownerService;
        this.propertyService = propertyService;
    }

    @PostMapping
    public ResponseEntity<OwnerResponse> createOwner(@Valid @RequestBody CreateOwnerRequest request) {
        var owner = ownerService.createOwner(request.getName(), request.getEmail());
        return ResponseEntity.status(HttpStatus.CREATED).body(OwnerResponse.from(owner));
    }

    @PostMapping("/{ownerId}/properties")
    public ResponseEntity<PropertyResponse> addProperty(@PathVariable("ownerId") UUID ownerId,
                                                        @Valid @RequestBody CreatePropertyRequest request) {
        var property = propertyService.addProperty(ownerId, request.toDetails());
        return ResponseEntity.created(URI.create("/properties/" + property.id()))
                .body(PropertyResponse.from(property));
    }
}
