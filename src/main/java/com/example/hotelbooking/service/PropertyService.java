package com.example.hotelbooking.service;

import com.example.hotelbooking.entity.Property;
import com.example.hotelbooking.entity.RoomType;
import com.example.hotelbooking.service.search.PropertySearchCriteria;

import java.math.BigDecimal;
import java.util.List;
import java.util.Set;
import java.util.UUID;

public interface PropertyService {
    Property addProperty(UUID ownerId, PropertyDetails details);

    Property getProperty(UUID id);

    List<SearchResult> search(PropertySearchCriteria criteria);

    record RoomDetails(String name, int guestCapacity, int totalRooms, BigDecimal nightlyPrice) {}
    record PropertyDetails(String name, String city, String locality, int stars,
                           Set<String> amenities, List<RoomDetails> roomTypes) {}
    record AvailableRoom(RoomType roomType, int availableRooms) {}
    record SearchResult(Property property, List<AvailableRoom> rooms) {}
}
