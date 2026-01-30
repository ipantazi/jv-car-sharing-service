package com.github.ipantazi.carsharing.service.user;

import com.github.ipantazi.carsharing.dto.user.UserChangePasswordDto;
import com.github.ipantazi.carsharing.dto.user.UserProfileUpdateDto;
import com.github.ipantazi.carsharing.dto.user.UserRegistrationRequestDto;
import com.github.ipantazi.carsharing.dto.user.UserResponseDto;
import com.github.ipantazi.carsharing.dto.user.UserRoleUpdateDto;
import com.github.ipantazi.carsharing.exception.RegistrationException;
import com.github.ipantazi.carsharing.model.User;
import java.util.Optional;

public interface UserService {
    UserResponseDto register(UserRegistrationRequestDto requestDto)
            throws RegistrationException;

    UserResponseDto updateUserRole(Long id, UserRoleUpdateDto newRole);

    UserResponseDto getUserDetails(Long userId);

    UserResponseDto updateUserProfile(Long userId, UserProfileUpdateDto userProfileUpdateDto);

    void changePassword(Long id, UserChangePasswordDto requestDto);

    Optional<Long> resolveUserIdForAccess(Long authUserId,
                                          User.Role userRole,
                                          Long requestedUserId);

    boolean canAccessRental(Long userId, Long rentalOwnerId);

    String getEmailByRentalId(Long userId);

    User getUserById(Long userId);
}
