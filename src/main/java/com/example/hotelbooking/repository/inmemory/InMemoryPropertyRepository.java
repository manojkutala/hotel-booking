package com.example.hotelbooking.repository.inmemory;

import com.example.hotelbooking.entity.Property;
import com.example.hotelbooking.repository.PropertyRepository;
import org.springframework.stereotype.Repository;

import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

@Repository
public class InMemoryPropertyRepository implements PropertyRepository {
    private final Map<UUID, Property> properties = new ConcurrentHashMap<>();

    @Override
    public Property save(Property property) {
        properties.put(property.id(), property);
        return property;
    }

    @Override
    public Optional<Property> findById(UUID id) {
        return Optional.ofNullable(properties.get(id));
    }

    @Override
    public List<Property> findAll() {
        return properties.values().stream()
                .sorted(Comparator.comparing(Property::name).thenComparing(Property::id)).toList();
    }
}
