package com.github.ipantazi.carsharing.dto.user;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record UserLoginRequestDto(
        @NotBlank(message = "Invalid email. Email shouldn't be blank.")
        @Email(message = "Invalid format email.")
        @Size(max = 50, message = "Email address should be exceed 50 characters.")
        String email,

        @NotBlank(message = "Invalid password. Password shouldn't be blank.")
        @Size(min = 8, max = 50, message = "Invalid password. "
                + "The password should be between 8 to 50.")
        String password
) {
}
