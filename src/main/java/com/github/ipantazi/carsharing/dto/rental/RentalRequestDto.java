package com.github.ipantazi.carsharing.dto.rental;

import com.fasterxml.jackson.annotation.JsonFormat;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import java.time.LocalDate;

public record RentalRequestDto(
        @NotNull(message = "Return date cannot be null")
        @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd")
        LocalDate returnDate,

        @NotNull(message = "Car ID cannot be null")
        @Positive(message = "Car ID must be a positive number")
        Long carId
) {
}
