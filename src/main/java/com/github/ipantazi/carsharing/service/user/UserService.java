package com.github.ipantazi.carsharing.service.user;

import com.github.ipantazi.carsharing.dto.user.UserRegistrationRequestDto;
import com.github.ipantazi.carsharing.dto.user.UserRegistrationResponseDto;
import com.github.ipantazi.carsharing.exception.RegistrationException;

public interface UserService {
    UserRegistrationResponseDto register(UserRegistrationRequestDto requestDto)
            throws RegistrationException;
}
