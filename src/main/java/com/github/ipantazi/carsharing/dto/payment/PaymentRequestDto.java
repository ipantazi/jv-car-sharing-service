package com.github.ipantazi.carsharing.dto.payment;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

public record PaymentRequestDto(
        @NotNull(message = "Rental ID cannot be null")
        @Positive(message = "Rental ID must be a positive number")
        Long rentalId,

        @NotNull(message = "Type cannot be null")
        @NotBlank(message = "Type cannot be blank")
        String type
) {
}
