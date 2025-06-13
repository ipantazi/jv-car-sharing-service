package com.github.ipantazi.carsharing.dto.rental;

import jakarta.validation.constraints.Min;

public record RentalRequestFilterDto(
        @Min(value = 1, message = "Invalid user id. Size should be greater than 0")
        Long user_id,

        Boolean is_active
) {
}
