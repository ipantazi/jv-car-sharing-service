package com.github.ipantazi.carsharing.dto.user;

//import com.github.ipantazi.carsharing.validation.FieldMatch;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/*@FieldMatch(field = "password",
        fieldMatch = "repeatPassword",
        message = "The passwords do not match."
)*/
public record UserRegistrationRequestDto(
        @Email(message = "Invalid format email.")
        @Size(max = 50, message = "Email address must not exceed 50 characters.")
        String email,

        @NotBlank(message = "Invalid password. Password shouldn't be blank.")
        @Size(min = 8, max = 50, message = "Invalid password. "
                + "The password should be between 8 to 50.")
        @Pattern(
                regexp = "^(?=.*[a-z])(?=.*[A-Z])(?=.*\\d)(?=.*[^\\w]).*$",
                message = "Password must include at least one lowercase letter, "
                        + "one uppercase letter, one number, and one special character."
        )
        String password,

        String repeatPassword,

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
