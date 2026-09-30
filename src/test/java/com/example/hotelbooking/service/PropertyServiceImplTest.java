package com.example.hotelbooking.service;

import com.example.hotelbooking.TestFixture;
import com.example.hotelbooking.entity.DateRange;
import com.example.hotelbooking.exception.BusinessException;
import com.example.hotelbooking.service.search.PropertyFilter;
import com.example.hotelbooking.service.search.PropertySearchCriteria;
import com.example.hotelbooking.service.search.StandardPropertyFilters;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import static com.example.hotelbooking.TestFixture.*;
import static org.assertj.core.api.Assertions.*;

class PropertyServiceImplTest {
    @Test
    void singleOwnerCanOnboardMultipleProperties() {
        var fixture = new TestFixture();
        var first = fixture.property(1);
        var second = fixture.property(2);
        assertThat(first.ownerId()).isEqualTo(second.ownerId()).isEqualTo(fixture.owner.id());
        assertThat(fixture.properties.findAll()).hasSize(2);
        assertThatThrownBy(() -> fixture.propertyService.addProperty(UUID.randomUUID(), details()))
                .isInstanceOf(BusinessException.class);
    }

    @Test
    void combinesLocationAmenitiesRatingAndPriceFilters() {
        var fixture = new TestFixture();
        var property = fixture.property(1);
        var matching = new PropertySearchCriteria("bENGALURU", " indiranagar ", STAY, 2,
                new BigDecimal("2000"), new BigDecimal("2000"), Set.of("WIFI", "Parking"), 4);
        assertThat(fixture.propertyService.search(matching)).hasSize(1);
        var missingAmenity = new PropertySearchCriteria("Bengaluru", null, STAY, 2,
                null, null, Set.of("pool"), null);
        assertThat(fixture.propertyService.search(missingAmenity)).isEmpty();
        fixture.book(property, STAY);
        assertThat(fixture.propertyService.search(matching)).isEmpty();
    }

    @Test
    void sameRoomMustSatisfyPriceCapacityAndAvailability() {
        var fixture = new TestFixture();
        var property = fixture.propertyService.addProperty(fixture.owner.id(), details());
        var cheapForFamily = new PropertySearchCriteria(null, null, STAY, 3,
                null, new BigDecimal("100"), Set.of(), null);
        assertThat(fixture.propertyService.search(cheapForFamily)).isEmpty();
        var cheapForOne = new PropertySearchCriteria(null, null, STAY, 1,
                null, new BigDecimal("100"), Set.of(), null);
        assertThat(fixture.propertyService.search(cheapForOne)).hasSize(1);
        fixture.bookingService.book(property.id(), property.roomTypes().getFirst().id(), STAY, 1, "G", "g@example.com");
        assertThat(fixture.propertyService.search(cheapForOne)).isEmpty();
        var family = new PropertySearchCriteria(null, null, STAY, 3, null, null, Set.of(), null);
        assertThat(fixture.propertyService.search(family).getFirst().rooms())
                .extracting(room -> room.roomType().name()).containsExactly("Family");
    }

    @Test
    void newFilterCanBeComposedWithoutChangingSearchService() {
        var fixture = new TestFixture();
        fixture.property(1);
        var filters = new ArrayList<PropertyFilter>(StandardPropertyFilters.all());
        filters.add((property, query) -> property.name().startsWith("Beach"));
        var service = new PropertyServiceImpl(fixture.owners, fixture.properties, fixture.availability,
                fixture.lock, filters, CURRENCY, CLOCK);
        assertThat(service.search(new PropertySearchCriteria(null, null, STAY, 1,
                null, null, Set.of(), null))).isEmpty();
    }

    @Test
    void rejectsInvertedPriceRangeAndPastStay() {
        assertThatThrownBy(() -> new PropertySearchCriteria(null, null, STAY, 1,
                new BigDecimal("100"), new BigDecimal("99"), Set.of(), null))
                .isInstanceOf(IllegalArgumentException.class);
        var fixture = new TestFixture();
        var past = new DateRange(STAY.checkIn().minusYears(1), STAY.checkOut().minusYears(1));
        assertThatThrownBy(() -> fixture.propertyService.search(new PropertySearchCriteria(null, null,
                past, 1, null, null, Set.of(), null))).isInstanceOf(IllegalArgumentException.class);
    }

    private PropertyService.PropertyDetails details() {
        return new PropertyService.PropertyDetails("Small Hotel", "Bengaluru", "Centre", 3, Set.of("wifi"),
                List.of(new PropertyService.RoomDetails("Single", 1, 1, new BigDecimal("100")),
                        new PropertyService.RoomDetails("Family", 4, 1, new BigDecimal("300"))));
    }
}
