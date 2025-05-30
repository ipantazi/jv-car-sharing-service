package com.github.ipantazi.carsharing.controller;

import com.github.ipantazi.carsharing.dto.user.UserChangePasswordDto;
import com.github.ipantazi.carsharing.dto.user.UserProfileUpdateDto;
import com.github.ipantazi.carsharing.dto.user.UserResponseDto;
import com.github.ipantazi.carsharing.dto.user.UserRoleUpdateDto;
import com.github.ipantazi.carsharing.security.CustomUserDetails;
import com.github.ipantazi.carsharing.service.user.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "User management", description = "Endpoints for managing users")
@RestController
@RequestMapping("/users")
@RequiredArgsConstructor
public class UserController {
    private final UserService userService;

    @PreAuthorize("hasRole('ROLE_MANAGER')")
    @PatchMapping("/{id}/role")
    @Operation(summary = "Update user role", description = "Update the role of a user by their ID")
    public UserResponseDto updateUserRole(@PathVariable Long id,
                                          @RequestBody @Valid UserRoleUpdateDto newRole) {
        return userService.updateUserRole(id, newRole);
    }

    @PreAuthorize("isAuthenticated()")
    @GetMapping("/me")
    @Operation(
            summary = "Get user details",
            description = "Retrieve the details of the authenticated user"
    )
    public UserResponseDto getUserDetails(Authentication authentication) {
        CustomUserDetails userDetails = (CustomUserDetails) authentication.getPrincipal();
        return userService.getUserDetails(userDetails.getId());
    }

    @PreAuthorize("isAuthenticated()")
    @PutMapping("/me")
    @Operation(
            summary = "Update profile info",
            description = "Update the profile information of the authenticated user"
    )
    public UserResponseDto updateUserProfile(
            Authentication authentication,
            @RequestBody @Valid UserProfileUpdateDto userProfileUpdateDto
    ) {
        CustomUserDetails userDetails = (CustomUserDetails) authentication.getPrincipal();
        return userService.updateUserProfile(userDetails.getId(), userProfileUpdateDto);
    }

    @PreAuthorize("isAuthenticated()")
    @PatchMapping("/me")
    @Operation(
            summary = "Change password",
            description = "Change the password of the authenticated user"
    )
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void changePassword(Authentication authentication,
                               @RequestBody @Valid UserChangePasswordDto requestDto) {
        CustomUserDetails userDetails = (CustomUserDetails) authentication.getPrincipal();
        userService.changePassword(userDetails.getId(), requestDto);
    }
}
