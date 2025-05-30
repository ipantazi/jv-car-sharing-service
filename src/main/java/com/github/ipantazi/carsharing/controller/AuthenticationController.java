package com.github.ipantazi.carsharing.controller;

import com.github.ipantazi.carsharing.dto.user.UserLoginRequestDto;
import com.github.ipantazi.carsharing.dto.user.UserLoginResponseDto;
import com.github.ipantazi.carsharing.dto.user.UserRegistrationRequestDto;
import com.github.ipantazi.carsharing.dto.user.UserResponseDto;
import com.github.ipantazi.carsharing.exception.RegistrationException;
import com.github.ipantazi.carsharing.security.AuthenticationService;
import com.github.ipantazi.carsharing.service.user.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "User management", description = "Endpoints of management users.")
@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
public class AuthenticationController {
    private final UserService userService;
    private final AuthenticationService authenticationService;

    @PostMapping("/registration")
    @Operation(summary = "Registration user", description = "Registration a new user")
    public UserResponseDto registerUser(
            @RequestBody
            @Valid
            UserRegistrationRequestDto requestDto
    ) throws RegistrationException {
        return userService.register(requestDto);
    }

    @PostMapping("/login")
    @Operation(summary = "Login user", description = "Authorization of the current user")
    public UserLoginResponseDto login(
            @RequestBody
            @Valid
            UserLoginRequestDto request
    ) {
        return authenticationService.authenticate(request);
    }
}
