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
import com.github.ipantazi.carsharing.model.User;
import com.github.ipantazi.carsharing.repository.user.UserRepository;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class UserServiceImpl implements UserService {
    private final UserRepository userRepository;
    private final UserMapper userMapper;
    private final PasswordEncoder passwordEncoder;

    @Override
    @Transactional
    public UserResponseDto register(UserRegistrationRequestDto requestDto)
            throws RegistrationException {
        if (userRepository.existsUserByEmail(requestDto.email())) {
            throw new RegistrationException(
                    "User with email %s already exists".formatted(requestDto.email()));
        }
        User user = userMapper.toUserEntity(requestDto);
        user.setPassword(passwordEncoder.encode(requestDto.password()));
        return userMapper.toUserDto(userRepository.save(user));
    }

    @Override
    @Transactional
    public UserResponseDto updateUserRole(Long id, UserRoleUpdateDto newRole) {
        User user = lockUserForUpdate(id);
        user.setRole(User.Role.valueOf(newRole.role()));
        return userMapper.toUserDto(userRepository.save(user));
    }

    @Override
    public UserResponseDto getUserDetails(Long userId) {
        User user = getUserById(userId);
        return userMapper.toUserDto(user);
    }

    @Override
    @Transactional
    public UserResponseDto updateUserProfile(Long userId,
                                             UserProfileUpdateDto userProfileUpdateDto) {
        User user = lockUserForUpdate(userId);

        boolean emailChanging = !user.getEmail().equals(userProfileUpdateDto.email());
        if (emailChanging) {
            boolean emailUsedByOtherUser =
                    userRepository.existsByEmailAndIdNot(userProfileUpdateDto.email(), userId);

            if (emailUsedByOtherUser) {
                throw new EmailAlreadyInUseException("Email already in use: "
                        + userProfileUpdateDto.email());
            }
        }
        userMapper.updateUserEntity(userProfileUpdateDto, user);
        return userMapper.toUserDto(userRepository.save(user));
    }

    @Override
    @Transactional
    public void changePassword(Long id, UserChangePasswordDto requestDto) {
        User user = lockUserForUpdate(id);
        if (!passwordEncoder.matches(requestDto.oldPassword(), user.getPassword())) {
            throw new InvalidOldPasswordException("Old password is incorrect");

        }
        user.setPassword(passwordEncoder.encode(requestDto.newPassword()));
        userRepository.save(user);
    }

    @Override
    public Optional<Long> resolveUserIdForAccess(Long authUserId,
                                                 User.Role userRole,
                                                 Long requestedUserId) {
        boolean isManager = userRole == User.Role.MANAGER;

        if (isManager && requestedUserId != null) {
            validateUserExistsOrThrow(requestedUserId);
            return Optional.of(requestedUserId);
        } else if (!isManager) {
            return Optional.of(authUserId);
        } else {
            return Optional.empty();
        }
    }

    @Override
    public boolean canAccessRental(Long userId, Long rentalOwnerId) {
        boolean isManager = userRepository.existsByIdAndRole(userId, User.Role.MANAGER);
        return isManager || userId.equals(rentalOwnerId);
    }

    @Override
    public String getEmailByRentalId(Long rentalId) {
        return userRepository.getEmailByRentalId(rentalId).orElseThrow(
                () -> new EntityNotFoundException("User email not found with rental id: "
                        + rentalId));
    }

    @Override
    public User getUserById(Long userId) {
        return userRepository.findById(userId)
                .orElseThrow(() ->
                        new EntityNotFoundException("User not found with id: " + userId)
                );
    }

    private User lockUserForUpdate(Long userId) {
        return userRepository.lockUserForUpdate(userId)
                .orElseThrow(() ->
                        new EntityNotFoundException("User not found with id: " + userId)
                );
    }

    private void validateUserExistsOrThrow(Long userId) {
        if (userRepository.existsSoftDeletedUserById(userId) == 1L) {
            throw new IllegalArgumentException("User with id: %d was previously deleted."
                    .formatted(userId));
        }
        if (!userRepository.existsById(userId)) {
            throw new EntityNotFoundException("Can't find user with id: " + userId);
        }
    }
}
