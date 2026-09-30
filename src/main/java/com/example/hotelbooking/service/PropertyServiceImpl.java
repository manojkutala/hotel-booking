package com.example.hotelbooking.service;

import com.example.hotelbooking.entity.*;
import com.example.hotelbooking.exception.BusinessException;
import com.example.hotelbooking.repository.OwnerRepository;
import com.example.hotelbooking.repository.PropertyRepository;
import com.example.hotelbooking.service.search.PropertyFilter;
import com.example.hotelbooking.service.search.PropertySearchCriteria;
import org.springframework.stereotype.Service;

import java.time.Clock;
import java.time.LocalDate;
import java.util.Currency;
import java.util.List;
import java.util.UUID;

@Service
public class PropertyServiceImpl implements PropertyService {
    private final OwnerRepository owners;
    private final PropertyRepository properties;
    private final AvailabilityService availability;
    private final InventoryLock inventoryLock;
    private final List<PropertyFilter> filters;
    private final Currency currency;
    private final Clock clock;

    public PropertyServiceImpl(OwnerRepository owners, PropertyRepository properties,
                               AvailabilityService availability, InventoryLock inventoryLock,
                               List<PropertyFilter> filters, Currency currency, Clock clock) {
        this.owners = owners;
        this.properties = properties;
        this.availability = availability;
        this.inventoryLock = inventoryLock;
        this.filters = List.copyOf(filters);
        this.currency = currency;
        this.clock = clock;
    }

    @Override
    public Property addProperty(UUID ownerId, PropertyDetails details) {
        owners.findById(ownerId).orElseThrow(() -> BusinessException.notFound("Owner"));
        var roomTypes = details.roomTypes().stream().map(room -> new RoomType(UUID.randomUUID(),
                room.name(), room.guestCapacity(), room.totalRooms(), new Money(room.nightlyPrice(), currency))).toList();
        return properties.save(new Property(UUID.randomUUID(), ownerId, details.name(), details.city(),
                details.locality(), details.stars(), details.amenities(), roomTypes));
    }

    @Override
    public Property getProperty(UUID id) {
        return properties.findById(id).orElseThrow(() -> BusinessException.notFound("Property"));
    }

    @Override
    public List<SearchResult> search(PropertySearchCriteria criteria) {
        criteria.stay().requireNotPast(LocalDate.now(clock));
        return inventoryLock.execute(() -> properties.findAll().stream()
                .filter(property -> filters.stream().allMatch(filter -> filter.matches(property, criteria)))
                .map(property -> new SearchResult(property, matchingRooms(property, criteria)))
                .filter(result -> !result.rooms().isEmpty()).toList());
    }

    private List<AvailableRoom> matchingRooms(Property property, PropertySearchCriteria criteria) {
        return property.roomTypes().stream()
                .filter(room -> room.accommodates(criteria.guests()))
                .filter(room -> criteria.minPrice() == null
                        || room.nightlyPrice().amount().compareTo(criteria.minPrice()) >= 0)
                .filter(room -> criteria.maxPrice() == null
                        || room.nightlyPrice().amount().compareTo(criteria.maxPrice()) <= 0)
                .map(room -> new AvailableRoom(room, availability.availableRooms(room, criteria.stay())))
                .filter(room -> room.availableRooms() > 0).toList();
    }
}
