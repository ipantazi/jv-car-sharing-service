package com.github.ipantazi.carsharing.dto.car;

import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;

public record UpdateCarDto(
        @NotBlank(message = "Invalid model. Model can't be blank.")
        @Size(
                min = 1,
                max = 50,
                message = "Invalid model. Model must be between 1 and 50 characters.")
        String model,

        @NotBlank(message = "Invalid brand. Brand can't be blank.")
        @Size(
                min = 3,
                max = 50,
                message = "Invalid brand. Brand must be between 3 and 50 characters.")
        String brand,

        @NotBlank(message = "Invalid type. Type can't be blank.")
        @Size(
                min = 3,
                max = 20,
                message = "Invalid type. Type must be between 3 and 20 characters.")
        String type,

        @NotNull(message = "Invalid daily fee. Daily fee shouldn't be null.")
        @Positive(message = "Invalid daily fee. Daily fee should be positive.")
        @Digits(
                integer = 10,
                fraction = 2,
                message = "Invalid daily fee. The maximum allowed number for a daily fee is "
                        + "10 digits and 2 digits after the decimal point.")
        BigDecimal dailyFee) {
}
