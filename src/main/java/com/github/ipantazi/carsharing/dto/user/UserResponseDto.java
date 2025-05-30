package com.github.ipantazi.carsharing.dto.user;

public record UserResponseDto(
        Long id,
        String email,
        String firstName,
        String lastName,
        String role
) {
}
