package com.github.ipantazi.carsharing.service.user;

import com.github.ipantazi.carsharing.dto.user.UserRegistrationRequestDto;
import com.github.ipantazi.carsharing.dto.user.UserRegistrationResponseDto;
import com.github.ipantazi.carsharing.exception.RegistrationException;
import com.github.ipantazi.carsharing.mapper.UserMapper;
import com.github.ipantazi.carsharing.model.User;
import com.github.ipantazi.carsharing.repository.user.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class UserServiceImpl implements UserService {
    private final UserRepository userRepository;
    private final UserMapper userMapper;
    private final PasswordEncoder passwordEncoder;

    @Override
    public UserRegistrationResponseDto register(UserRegistrationRequestDto requestDto)
            throws RegistrationException {
        if (userRepository.existsByEmail(requestDto.email())) {
            throw new RegistrationException("Can't register user with this email: "
                    + requestDto.email());
        }

        User user = userMapper.toUserEntity(requestDto);
        user.setPassword(passwordEncoder.encode(requestDto.password()));
        return userMapper.toUserDto(userRepository.save(user));
    }
}
