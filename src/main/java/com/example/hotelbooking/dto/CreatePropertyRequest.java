package com.example.hotelbooking.dto;

import com.example.hotelbooking.service.PropertyService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.List;
import java.util.Set;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class CreatePropertyRequest {
    @NotBlank
    private String name;

    @NotBlank
    private String city;

    @NotBlank
    private String locality;

    @Min(1)
    @Max(5)
    private int stars;

    @NotNull
    private Set<@NotBlank String> amenities;

    @NotEmpty
    private List<@NotNull @Valid RoomTypeRequest> roomTypes;

    public PropertyService.PropertyDetails toDetails() {
        return new PropertyService.PropertyDetails(name, city, locality, stars, amenities,
                roomTypes.stream().map(room -> new PropertyService.RoomDetails(room.getName(),
                        room.getGuestCapacity(), room.getTotalRooms(), room.getNightlyPrice())).toList());
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class RoomTypeRequest {
        @NotBlank
        private String name;

        @Positive
        private int guestCapacity;

        @Positive
        private int totalRooms;

        @NotNull
        @DecimalMin("0.01")
        @Digits(integer = 10, fraction = 2)
        private BigDecimal nightlyPrice;
    }
}
