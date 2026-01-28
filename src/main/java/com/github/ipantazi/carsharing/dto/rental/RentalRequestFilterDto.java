package com.github.ipantazi.carsharing.dto.rental;

import jakarta.validation.constraints.Positive;

public record RentalRequestFilterDto(
        @Positive(message = "Invalid user id. User id should be a positive number")
        Long user_id,

        Boolean is_active
) {
}
