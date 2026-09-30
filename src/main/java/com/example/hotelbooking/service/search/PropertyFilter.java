package com.example.hotelbooking.service.search;

import com.example.hotelbooking.entity.Property;

@FunctionalInterface
public interface PropertyFilter {
    boolean matches(Property property, PropertySearchCriteria criteria);
}
