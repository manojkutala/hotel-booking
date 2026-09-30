package com.example.hotelbooking.controller;

import com.example.hotelbooking.dto.PropertyResponse;
import com.example.hotelbooking.dto.PropertySearchRequest;
import com.example.hotelbooking.dto.PropertySearchResponse;
import com.example.hotelbooking.service.PropertyService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/properties")
@Validated
public class PropertyController {
    private final PropertyService propertyService;

    public PropertyController(PropertyService propertyService) {
        this.propertyService = propertyService;
    }

    @GetMapping
    public ResponseEntity<List<PropertySearchResponse>> searchProperties(
            @Valid @ModelAttribute PropertySearchRequest request) {
        var results = propertyService.search(request.toCriteria()).stream()
                .map(PropertySearchResponse::from).toList();
        return ResponseEntity.ok(results);
    }

    @GetMapping("/{propertyId}")
    public ResponseEntity<PropertyResponse> getProperty(@PathVariable("propertyId") UUID propertyId) {
        return ResponseEntity.ok(PropertyResponse.from(propertyService.getProperty(propertyId)));
    }
}
