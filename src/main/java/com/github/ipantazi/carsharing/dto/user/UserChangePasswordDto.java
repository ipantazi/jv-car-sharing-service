package com.github.ipantazi.carsharing.dto.user;

import com.github.ipantazi.carsharing.validation.FieldMatch;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

@FieldMatch(field = "newPassword",
        fieldMatch = "repeatPassword",
        message = "The passwords do not match."
)
public record UserChangePasswordDto(
        @NotBlank(message = "Old password is required")
        String oldPassword,

        @NotBlank(message = "New password is required")
        @Size(min = 8, max = 50, message = "Invalid new password. "
                + "The password should be between 8 to 50 characters.")
        @Pattern(
                regexp = "^(?=.*[a-z])(?=.*[A-Z])(?=.*\\d)(?=.*[^\\w]).*$",
                message = "Password must include at least one lowercase letter, "
                        + "one uppercase letter, one number, and one special character."
        )
        String newPassword,

        String repeatPassword
) {
}
