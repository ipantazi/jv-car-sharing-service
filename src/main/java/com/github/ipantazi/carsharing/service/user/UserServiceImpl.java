package com.github.ipantazi.carsharing.service.user;

import com.github.ipantazi.carsharing.dto.user.UserChangePasswordDto;
import com.github.ipantazi.carsharing.dto.user.UserProfileUpdateDto;
import com.github.ipantazi.carsharing.dto.user.UserRegistrationRequestDto;
import com.github.ipantazi.carsharing.dto.user.UserResponseDto;
import com.github.ipantazi.carsharing.dto.user.UserRoleUpdateDto;
import com.github.ipantazi.carsharing.exception.EmailAlreadyInUseException;
import com.github.ipantazi.carsharing.exception.EntityNotFoundException;
import com.github.ipantazi.carsharing.exception.InvalidOldPasswordException;
import com.github.ipantazi.carsharing.exception.RegistrationException;
import com.github.ipantazi.carsharing.mapper.UserMapper;
import com.github.ipantazi.carsharing.model.Rental;
import com.github.ipantazi.carsharing.model.User;
import com.github.ipantazi.carsharing.repository.user.UserRepository;
import com.github.ipantazi.carsharing.security.CustomUserDetails;
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
    public UserResponseDto register(UserRegistrationRequestDto requestDto)
            throws RegistrationException {
        if (userRepository.existsByEmail(requestDto.email())) {
            throw new RegistrationException("Can't register user with this email: "
                    + requestDto.email());
        }

        User user = userMapper.toUserEntity(requestDto);
        user.setPassword(passwordEncoder.encode(requestDto.password()));
        return userMapper.toUserDto(userRepository.save(user));
    }

    @Override
    public UserResponseDto updateUserRole(Long id, UserRoleUpdateDto newRole) {
        User user = getUserById(id);
        user.setRole(User.Role.valueOf(newRole.role()));
        return userMapper.toUserDto(userRepository.save(user));
    }

    @Override
    public UserResponseDto getUserDetails(Long userId) {
        User user = getUserById(userId);
        return userMapper.toUserDto(user);
    }

    @Override
    public UserResponseDto updateUserProfile(Long userId,
                                             UserProfileUpdateDto userProfileUpdateDto) {
        User user = getUserById(userId);
        if (!user.getEmail().equals(userProfileUpdateDto.email())
                && userRepository.existsByEmail(userProfileUpdateDto.email())) {
            throw new EmailAlreadyInUseException("Email already in use: "
                    + userProfileUpdateDto.email());
        }
        userMapper.updateUserEntity(userProfileUpdateDto, user);
        return userMapper.toUserDto(userRepository.save(user));
    }

    @Override
    public void changePassword(Long id, UserChangePasswordDto requestDto) {
        User user = getUserById(id);
        if (!passwordEncoder.matches(requestDto.oldPassword(), user.getPassword())) {
            throw new InvalidOldPasswordException("Old password is incorrect");

        }
        user.setPassword(passwordEncoder.encode(requestDto.newPassword()));
        userRepository.save(user);
    }

    @Override
    public boolean validateUserExistsOrThrow(Long userId) {
        if (userRepository.existsSoftDeletedUserById(userId) == 1L) {
            throw new IllegalArgumentException("User with id: %d was previously deleted."
                    .formatted(userId));
        }
        if (!userRepository.existsById(userId)) {
            throw new EntityNotFoundException("Can't find user with id: " + userId);
        }
        return true;
    }

    @Override
    public Long resolveUserIdForAccess(CustomUserDetails userDetails, Long requestedUserId) {
        boolean isManager = userDetails.getRole().equals(User.Role.MANAGER);

        if (isManager && requestedUserId != null) {
            validateUserExistsOrThrow(requestedUserId);
            return requestedUserId;
        } else if (!isManager) {
            return userDetails.getId();
        } else {
            return null;
        }
    }

    @Override
    public boolean canAccessRental(Long userId, Rental rental) {
        Long rentalUserId = rental.getUserId();
        return isManager(userId) || userId.equals(rentalUserId);
    }

    private boolean isManager(Long userId) {
        return getUserById(userId).getRole().equals(User.Role.MANAGER);
    }

    private User getUserById(Long userId) {
        return userRepository.findById(userId)
                .orElseThrow(() ->
                        new EntityNotFoundException("User not found with id: " + userId)
                );
    }
}
