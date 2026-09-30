package com.example.hotelbooking.service.search;

import com.example.hotelbooking.entity.DateRange;

import java.math.BigDecimal;
import java.util.Locale;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

public record PropertySearchCriteria(String city, String locality, DateRange stay, int guests,
                                     BigDecimal minPrice, BigDecimal maxPrice,
                                     Set<String> amenities, Integer minStars) {
    public PropertySearchCriteria {
        Objects.requireNonNull(stay, "stay");
        if (guests < 1) {
            throw new IllegalArgumentException("Guests must be positive");
        }
        if ((minPrice != null && minPrice.signum() < 0) || (maxPrice != null && maxPrice.signum() < 0)
                || (minPrice != null && maxPrice != null && minPrice.compareTo(maxPrice) > 0)) {
            throw new IllegalArgumentException("Price range must be nonnegative with minPrice <= maxPrice");
        }
        if (minStars != null && (minStars < 1 || minStars > 5)) {
            throw new IllegalArgumentException("Minimum stars must be between 1 and 5");
        }
        city = city == null ? null : city.trim();
        locality = locality == null ? null : locality.trim();
        amenities = amenities == null ? Set.of() : amenities.stream()
                .map(value -> value.trim().toLowerCase(Locale.ROOT)).collect(Collectors.toUnmodifiableSet());
    }
}
