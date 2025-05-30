package com.github.ipantazi.carsharing.dto.user;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record UserProfileUpdateDto(
        @NotBlank(message = "Invalid email. Email shouldn't be blank.")
        @Email(message = "Invalid format email.")
        @Size(max = 50, message = "Email address must not exceed 50 characters.")
        String email,

        @NotBlank(message = "Invalid first name. First name shouldn't be blank.")
        @Size(min = 3, max = 50, message = "Invalid first name. "
                + "First name should be between 3 to 50.")
        @Pattern(regexp = "^[a-zA-Z]*$", message = "First name must contain only letters.")
        String firstName,

        @NotBlank(message = "Invalid last name. Last name shouldn't be blank.")
        @Size(min = 3, max = 50, message = "Invalid last name. "
                + "Last name should be between 3 to 50.")
        @Pattern(regexp = "^[a-zA-Z]*$", message = "Last name must be contain only letters.")
        String lastName
) {
}
