package com.example.hotelbooking.service.search;

import java.util.List;

public final class StandardPropertyFilters {
    private StandardPropertyFilters() {}

    public static List<PropertyFilter> all() {
        return List.of(city(), locality(), stars(), amenities());
    }

    public static PropertyFilter city() {
        return (property, query) -> query.city() == null || property.city().equalsIgnoreCase(query.city());
    }

    public static PropertyFilter locality() {
        return (property, query) -> query.locality() == null || property.locality().equalsIgnoreCase(query.locality());
    }

    public static PropertyFilter stars() {
        return (property, query) -> query.minStars() == null || property.stars() >= query.minStars();
    }

    public static PropertyFilter amenities() {
        return (property, query) -> property.amenities().containsAll(query.amenities());
    }
}
