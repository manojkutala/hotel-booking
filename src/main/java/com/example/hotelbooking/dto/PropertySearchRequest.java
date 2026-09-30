package com.example.hotelbooking.dto;

import com.example.hotelbooking.entity.DateRange;
import com.example.hotelbooking.service.search.PropertySearchCriteria;
import jakarta.validation.constraints.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Set;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class PropertySearchRequest {
    private String city;

    private String locality;

    @NotNull
    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
    private LocalDate checkIn;

    @NotNull
    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
    private LocalDate checkOut;

    @NotNull
    @Positive
    private Integer guests;

    @DecimalMin("0")
    private BigDecimal minPrice;

    @DecimalMin("0")
    private BigDecimal maxPrice;

    private Set<@NotBlank String> amenities;

    @Min(1)
    @Max(5)
    private Integer minStars;

    public PropertySearchCriteria toCriteria() {
        return new PropertySearchCriteria(city, locality, new DateRange(checkIn, checkOut),
                guests, minPrice, maxPrice, amenities, minStars);
    }
}
