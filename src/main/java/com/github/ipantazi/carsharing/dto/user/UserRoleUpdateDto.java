package com.github.ipantazi.carsharing.dto.user;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record UserRoleUpdateDto(
        @NotBlank(message = "Invalid role. Role can't be blank.")
        @Size(
                min = 5,
                max = 20,
                message = "Invalid role. Role must be between 5 and 20 characters."
        )
        String role
) {
}
