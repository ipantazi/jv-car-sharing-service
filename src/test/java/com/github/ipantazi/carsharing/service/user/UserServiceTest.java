package com.github.ipantazi.carsharing.service.user;

import static com.github.ipantazi.carsharing.util.TestDataUtil.B_CRYPT_PASSWORD;
import static com.github.ipantazi.carsharing.util.TestDataUtil.EMAIL_DOMAIN;
import static com.github.ipantazi.carsharing.util.TestDataUtil.EXISTING_EMAIL;
import static com.github.ipantazi.carsharing.util.TestDataUtil.EXISTING_ID_ANOTHER_USER;
import static com.github.ipantazi.carsharing.util.TestDataUtil.EXISTING_RENTAL_ID;
import static com.github.ipantazi.carsharing.util.TestDataUtil.EXISTING_USER_ID;
import static com.github.ipantazi.carsharing.util.TestDataUtil.FALSE_STATUS_FOR_SOFT_DELETED_USER;
import static com.github.ipantazi.carsharing.util.TestDataUtil.FIRST_NAME;
import static com.github.ipantazi.carsharing.util.TestDataUtil.LAST_NAME;
import static com.github.ipantazi.carsharing.util.TestDataUtil.NEW_EMAIL;
import static com.github.ipantazi.carsharing.util.TestDataUtil.NEW_NOT_HASHED_PASSWORD;
import static com.github.ipantazi.carsharing.util.TestDataUtil.NEW_USER_ID;
import static com.github.ipantazi.carsharing.util.TestDataUtil.NOT_EXISTING_NOT_HASHED_PASSWORD;
import static com.github.ipantazi.carsharing.util.TestDataUtil.NOT_EXISTING_RENTAL_ID;
import static com.github.ipantazi.carsharing.util.TestDataUtil.NOT_EXISTING_USER_ID;
import static com.github.ipantazi.carsharing.util.TestDataUtil.NOT_HASHED_PASSWORD;
import static com.github.ipantazi.carsharing.util.TestDataUtil.SAFE_DELETED_USER_ID;
import static com.github.ipantazi.carsharing.util.TestDataUtil.TRUE_STATUS_FOR_SOFT_DELETED_USER;
import static com.github.ipantazi.carsharing.util.TestDataUtil.USER_DTO_IGNORING_FIELD;
import static com.github.ipantazi.carsharing.util.TestDataUtil.createTestChangePasswordRequestDto;
import static com.github.ipantazi.carsharing.util.TestDataUtil.createTestUpdateUserDto;
import static com.github.ipantazi.carsharing.util.TestDataUtil.createTestUser;
import static com.github.ipantazi.carsharing.util.TestDataUtil.createTestUserRegistrationRequestDto;
import static com.github.ipantazi.carsharing.util.TestDataUtil.createTestUserResponseDto;
import static com.github.ipantazi.carsharing.util.assertions.TestAssertionsUtil.assertObjectsAreEqualIgnoringFields;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

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
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

@ExtendWith(MockitoExtension.class)
public class UserServiceTest {
    @Mock
    private UserRepository userRepository;
    @Mock
    private UserMapper userMapper;
    @Mock
    private PasswordEncoder passwordEncoder;
    @InjectMocks
    private UserServiceImpl userService;

    @Test
    @DisplayName("Test that a user can be registered successfully.")
    public void register_ValidUserRegistrationRequestDto_ReturnsUserRegistrationResponseDto() {
        //Given
        UserResponseDto expectedUserResponseDto = createTestUserResponseDto(
                NEW_USER_ID);
        User user = createTestUser(expectedUserResponseDto);
        UserRegistrationRequestDto userRequestDto = createTestUserRegistrationRequestDto(
                expectedUserResponseDto);

        when(userRepository.existsUserByEmail(userRequestDto.email())).thenReturn(false);
        when(userMapper.toUserEntity(userRequestDto)).thenReturn(user);
        when(passwordEncoder.encode(userRequestDto.password())).thenReturn(user.getPassword());
        when(userRepository.save(user)).thenReturn(user);
        when(userMapper.toUserDto(user)).thenReturn(expectedUserResponseDto);

        //When
        UserResponseDto actualUserResponseDto = userService.register(userRequestDto);

        //Then
        assertObjectsAreEqualIgnoringFields(
                actualUserResponseDto,
                expectedUserResponseDto,
                USER_DTO_IGNORING_FIELD
        );

        verify(userRepository, times(1)).existsUserByEmail(userRequestDto.email());
        verify(userMapper, times(1)).toUserEntity(userRequestDto);
        verify(passwordEncoder, times(1)).encode(userRequestDto.password());
        verify(userRepository, times(1)).save(user);
        verify(userMapper, times(1)).toUserDto(user);
        verifyNoMoreInteractions(userRepository, userMapper, passwordEncoder);
    }

    @Test
    @DisplayName("Verify that an exception is throw when an email already exists.")
    public void register_UserEmailAlreadyExists_ThrowsException() {
        //Given
        UserRegistrationRequestDto userRequestDto = createTestUserRegistrationRequestDto(
                EXISTING_USER_ID);

        when(userRepository.existsUserByEmail(userRequestDto.email())).thenReturn(true);

        //When
        assertThatThrownBy(() -> userService.register(userRequestDto))
                .isInstanceOf(RegistrationException.class)
                .hasMessage("User with email %s already exists".formatted(userRequestDto.email()));
        //Then

        verify(userRepository, times(1)).existsUserByEmail(userRequestDto.email());
        verify(userRepository, never()).save(any(User.class));
        verifyNoMoreInteractions(userRepository);
        verifyNoInteractions(userMapper, passwordEncoder);
    }

    @Test
    @DisplayName("Test update user role when request is valid.")
    public void updateUserRole_ValidRequest_ReturnsUpdatedUser() {
        // Given
        UserResponseDto expectedUserResponseDto = createTestUserResponseDto(EXISTING_USER_ID);
        User user = createTestUser(EXISTING_USER_ID);
        UserRoleUpdateDto userRoleUpdateDto = new UserRoleUpdateDto(
                expectedUserResponseDto.role());
        when(userRepository.lockUserForUpdate(EXISTING_USER_ID)).thenReturn(Optional.of(user));
        when(userRepository.save(user)).thenReturn(user);
        when(userMapper.toUserDto(user)).thenReturn(expectedUserResponseDto);

        // When
        UserResponseDto actualUserResponseDto = userService.updateUserRole(
                EXISTING_USER_ID, userRoleUpdateDto);

        // Then
        assertObjectsAreEqualIgnoringFields(
                actualUserResponseDto,
                expectedUserResponseDto,
                USER_DTO_IGNORING_FIELD
        );
        verify(userRepository, times(1)).lockUserForUpdate(EXISTING_USER_ID);
        verify(userRepository, times(1)).save(user);
        verify(userMapper, times(1)).toUserDto(user);
        verifyNoMoreInteractions(userRepository, userMapper);
    }

    @Test
    @DisplayName("Test update user role when user does not exist.")
    public void updateUserRole_UserNotFound_ThrowsException() {
        // Given
        UserRoleUpdateDto userRoleUpdateDto = new UserRoleUpdateDto(
                User.Role.MANAGER.toString());
        when(userRepository.lockUserForUpdate(NOT_EXISTING_USER_ID)).thenReturn(Optional.empty());

        // When & Then
        assertThatThrownBy(() -> userService.updateUserRole(
                NOT_EXISTING_USER_ID,
                userRoleUpdateDto
        ))
                .isInstanceOf(EntityNotFoundException.class)
                .hasMessageContaining("User not found with id: " + NOT_EXISTING_USER_ID);

        verify(userRepository, times(1)).lockUserForUpdate(NOT_EXISTING_USER_ID);
        verify(userRepository, never()).save(any(User.class));
        verifyNoMoreInteractions(userRepository);
        verifyNoInteractions(userMapper);
    }

    @Test
    @DisplayName("Test update user role when role is invalid.")
    public void updateUserRole_InvalidRole_ThrowsException() {
        // Given
        UserRoleUpdateDto userRoleUpdateDto = new UserRoleUpdateDto("INVALID_ROLE");
        User user = createTestUser(EXISTING_USER_ID);
        when(userRepository.lockUserForUpdate(EXISTING_USER_ID)).thenReturn(Optional.of(user));

        // When & Then
        assertThatThrownBy(() -> userService.updateUserRole(
                EXISTING_USER_ID,
                userRoleUpdateDto
        ))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("No enum constant "
                        + "com.github.ipantazi.carsharing.model.User.Role.INVALID_ROLE");

        verify(userRepository, times(1)).lockUserForUpdate(EXISTING_USER_ID);
        verify(userRepository, never()).save(any(User.class));
        verifyNoMoreInteractions(userRepository);
        verifyNoInteractions(userMapper);
    }

    @Test
    @DisplayName("Test get user details when user exists.")
    public void getUserDetails_UserExists_ReturnsUserDetails() {
        // Given
        UserResponseDto expectedUserResponseDto = createTestUserResponseDto(EXISTING_USER_ID);
        User user = createTestUser(expectedUserResponseDto);
        when(userRepository.findById(EXISTING_USER_ID)).thenReturn(Optional.of(user));
        when(userMapper.toUserDto(user)).thenReturn(expectedUserResponseDto);

        // When
        UserResponseDto actualUserResponseDto = userService.getUserDetails(EXISTING_USER_ID);

        // Then
        assertObjectsAreEqualIgnoringFields(
                actualUserResponseDto,
                expectedUserResponseDto,
                USER_DTO_IGNORING_FIELD
        );
        verify(userRepository, times(1)).findById(EXISTING_USER_ID);
        verify(userMapper, times(1)).toUserDto(user);
        verifyNoMoreInteractions(userRepository, userMapper);
    }

    @Test
    @DisplayName("Test get user details when user does not exist.")
    public void getUserDetails_UserDoesNotExist_ThrowsException() {
        // Given
        when(userRepository.findById(NOT_EXISTING_USER_ID)).thenReturn(Optional.empty());

        // When & Then
        assertThatThrownBy(() -> userService.getUserDetails(NOT_EXISTING_USER_ID))
                .isInstanceOf(EntityNotFoundException.class)
                .hasMessageContaining("User not found with id: " + NOT_EXISTING_USER_ID);

        verify(userRepository, times(1)).findById(NOT_EXISTING_USER_ID);
        verifyNoMoreInteractions(userRepository);
        verifyNoInteractions(userMapper);
    }

    @Test
    @DisplayName("Test update user profile when request is valid.")
    public void updateUserProfile_ValidRequest_ReturnsUpdatedUser() {
        // Given
        User user = createTestUser(EXISTING_USER_ID);
        UserResponseDto expectedUserResponseDto = new UserResponseDto(
                EXISTING_USER_ID,
                NEW_EMAIL,
                FIRST_NAME,
                LAST_NAME,
                user.getRole().toString()
        );
        UserProfileUpdateDto userProfileUpdateDto = createTestUpdateUserDto(
                expectedUserResponseDto);
        when(userRepository.lockUserForUpdate(EXISTING_USER_ID)).thenReturn(Optional.of(user));
        when(userRepository.existsByEmailAndIdNot(NEW_EMAIL, EXISTING_USER_ID)).thenReturn(false);
        when(userRepository.save(user)).thenReturn(user);
        when(userMapper.toUserDto(user)).thenReturn(expectedUserResponseDto);

        // When
        UserResponseDto actualUserResponseDto = userService.updateUserProfile(
                EXISTING_USER_ID, userProfileUpdateDto);

        // Then
        assertObjectsAreEqualIgnoringFields(
                actualUserResponseDto,
                expectedUserResponseDto,
                USER_DTO_IGNORING_FIELD
        );
        verify(userRepository, times(1)).lockUserForUpdate(EXISTING_USER_ID);
        verify(userRepository, times(1))
                .existsByEmailAndIdNot(NEW_EMAIL, EXISTING_USER_ID);
        verify(userMapper, times(1)).updateUserEntity(userProfileUpdateDto, user);
        verify(userRepository, times(1)).save(user);
        verify(userMapper, times(1)).toUserDto(user);
        verifyNoMoreInteractions(userRepository, userMapper);
    }

    @Test
    @DisplayName("Test update user profile when user does not exist.")
    public void updateUserProfile_UserDoesNotExist_ThrowsException() {
        // Given
        UserProfileUpdateDto userProfileUpdateDto = createTestUpdateUserDto(
                createTestUserResponseDto(NOT_EXISTING_USER_ID));
        when(userRepository.lockUserForUpdate(NOT_EXISTING_USER_ID)).thenReturn(Optional.empty());

        // When & Then
        assertThatThrownBy(() -> userService.updateUserProfile(
                NOT_EXISTING_USER_ID, userProfileUpdateDto))
                .isInstanceOf(EntityNotFoundException.class)
                .hasMessageContaining("User not found with id: " + NOT_EXISTING_USER_ID);

        verify(userRepository, times(1)).lockUserForUpdate(NOT_EXISTING_USER_ID);
        verifyNoMoreInteractions(userRepository);
        verifyNoInteractions(userMapper);
    }

    @Test
    @DisplayName("Test update user profile when new email is already in use.")
    public void updateUserProfile_WhenNewEmailAlreadyInUse_ThrowsException() {
        // Given
        User user = createTestUser(EXISTING_USER_ID);
        String anotherUsersExistingEmail = EXISTING_ID_ANOTHER_USER + EMAIL_DOMAIN;
        UserProfileUpdateDto userProfileUpdateDto = new UserProfileUpdateDto(
                anotherUsersExistingEmail,
                FIRST_NAME,
                LAST_NAME
        );
        when(userRepository.lockUserForUpdate(EXISTING_USER_ID)).thenReturn(Optional.of(user));
        when(userRepository.existsByEmailAndIdNot(anotherUsersExistingEmail, EXISTING_USER_ID))
                .thenReturn(true);

        // When & Then
        assertThatThrownBy(() -> userService.updateUserProfile(
                EXISTING_USER_ID, userProfileUpdateDto))
                .isInstanceOf(EmailAlreadyInUseException.class)
                .hasMessageContaining("Email already in use: " + userProfileUpdateDto.email());
        verify(userRepository, times(1)).lockUserForUpdate(EXISTING_USER_ID);
        verify(userRepository, times(1))
                .existsByEmailAndIdNot(anotherUsersExistingEmail, EXISTING_USER_ID);
        verifyNoMoreInteractions(userRepository);
        verifyNoInteractions(userMapper);
    }

    @Test
    @DisplayName("Test update user profile when new email is equal to old e-mail.")
    public void updateUserProfile_EmailEqualToOldEmail_ReturnsUpdatedUser() {
        // Given
        UserResponseDto expectedUserResponseDto = createTestUserResponseDto(EXISTING_USER_ID);
        User user = createTestUser(expectedUserResponseDto);
        UserProfileUpdateDto userProfileUpdateDto = createTestUpdateUserDto(
                expectedUserResponseDto);
        when(userRepository.lockUserForUpdate(EXISTING_USER_ID)).thenReturn(Optional.of(user));
        when(userRepository.save(user)).thenReturn(user);
        when(userMapper.toUserDto(user)).thenReturn(expectedUserResponseDto);

        // When
        UserResponseDto actualUserResponseDto = userService.updateUserProfile(
                EXISTING_USER_ID, userProfileUpdateDto);

        // Then
        assertObjectsAreEqualIgnoringFields(
                actualUserResponseDto,
                expectedUserResponseDto,
                USER_DTO_IGNORING_FIELD
        );
        verify(userRepository, times(1)).lockUserForUpdate(EXISTING_USER_ID);
        verify(userMapper, times(1)).updateUserEntity(userProfileUpdateDto, user);
        verify(userRepository, times(1)).save(user);
        verify(userMapper, times(1)).toUserDto(user);
        verifyNoMoreInteractions(userRepository, userMapper);
    }

    @Test
    @DisplayName("Test change password when request is valid.")
    public void changePassword_ValidRequest_ChangesPassword() {
        // Given
        User user = createTestUser(EXISTING_USER_ID);
        UserChangePasswordDto requestDto = createTestChangePasswordRequestDto();
        when(userRepository.lockUserForUpdate(EXISTING_USER_ID)).thenReturn(Optional.of(user));
        when(passwordEncoder.matches(NOT_HASHED_PASSWORD, B_CRYPT_PASSWORD))
                .thenReturn(true);
        when(passwordEncoder.encode(requestDto.newPassword())).thenReturn(NEW_NOT_HASHED_PASSWORD);

        // When
        userService.changePassword(EXISTING_USER_ID, requestDto);

        // Then
        verify(userRepository, times(1)).lockUserForUpdate(EXISTING_USER_ID);
        verify(passwordEncoder, times(1)).matches(NOT_HASHED_PASSWORD, B_CRYPT_PASSWORD);
        verify(passwordEncoder, times(1)).encode(requestDto.newPassword());
        verify(userRepository, times(1)).save(user);
        verifyNoMoreInteractions(userRepository, passwordEncoder);
    }

    @Test
    @DisplayName("Test change password when user does not exist.")
    public void changePassword_UserDoesNotExist_ThrowsException() {
        // Given
        UserChangePasswordDto requestDto = createTestChangePasswordRequestDto();
        when(userRepository.lockUserForUpdate(NOT_EXISTING_USER_ID)).thenReturn(Optional.empty());

        // When & Then
        assertThatThrownBy(() -> userService.changePassword(
                NOT_EXISTING_USER_ID, requestDto))
                .isInstanceOf(EntityNotFoundException.class)
                .hasMessageContaining("User not found with id: " + NOT_EXISTING_USER_ID);

        verify(userRepository, times(1)).lockUserForUpdate(NOT_EXISTING_USER_ID);
        verify(userRepository, never()).save(any(User.class));
        verifyNoMoreInteractions(userRepository);
        verifyNoInteractions(passwordEncoder);
    }

    @Test
    @DisplayName("Test change password when old password is invalid.")
    public void changePassword_InvalidOldPassword_ThrowsException() {
        // Given
        User user = createTestUser(EXISTING_USER_ID);
        UserChangePasswordDto requestDto = new UserChangePasswordDto(
                NOT_EXISTING_NOT_HASHED_PASSWORD,
                NEW_NOT_HASHED_PASSWORD,
                NEW_NOT_HASHED_PASSWORD
        );
        when(userRepository.lockUserForUpdate(EXISTING_USER_ID)).thenReturn(Optional.of(user));
        when(passwordEncoder.matches(NOT_EXISTING_NOT_HASHED_PASSWORD, B_CRYPT_PASSWORD))
                .thenReturn(false);

        // When & Then
        assertThatThrownBy(() -> userService.changePassword(
                EXISTING_USER_ID, requestDto))
                .isInstanceOf(InvalidOldPasswordException.class)
                .hasMessageContaining("Old password is incorrect");

        verify(userRepository, times(1)).lockUserForUpdate(EXISTING_USER_ID);
        verify(passwordEncoder, times(1)).matches(
                NOT_EXISTING_NOT_HASHED_PASSWORD,
                B_CRYPT_PASSWORD
        );
        verify(userRepository, never()).save(any(User.class));
        verifyNoMoreInteractions(userRepository, passwordEncoder);
        verifyNoInteractions(userMapper);
    }

    @Test
    @DisplayName("Test getUserById() method when user exists.")
    public void getUserById_ExistingUser_ReturnsUser() {
        //Given
        User expectedUser = createTestUser(EXISTING_USER_ID);

        when(userRepository.findById(EXISTING_USER_ID)).thenReturn(Optional.of(expectedUser));

        //When
        User actualUser = userService.getUserById(EXISTING_USER_ID);

        //Then
        assertObjectsAreEqualIgnoringFields(
                actualUser,
                expectedUser,
                USER_DTO_IGNORING_FIELD
        );
        verify(userRepository, times(1)).findById(EXISTING_USER_ID);
        verifyNoMoreInteractions(userRepository);
    }

    @Test
    @DisplayName("Test getUserById() method when user does not exist.")
    public void getUserById_NonExistingUser_ThrowsException() {
        //Given
        when(userRepository.findById(NOT_EXISTING_USER_ID)).thenReturn(Optional.empty());

        //When & Then
        assertThatThrownBy(() -> userService.getUserById(NOT_EXISTING_USER_ID))
                .isInstanceOf(EntityNotFoundException.class)
                .hasMessage("User not found with id: " + NOT_EXISTING_USER_ID);

        verify(userRepository, times(1)).findById(NOT_EXISTING_USER_ID);
        verifyNoMoreInteractions(userRepository);
    }

    @Test
    @DisplayName("""
            Test resolveUserIdForAccess() method when user is not a MANAGER and ignoring
             requestedUserId.
            """)
    public void resolveUserIdForAccess_NotManager_returnsAuthUserId() {
        //Given
        User user = createTestUser(EXISTING_USER_ID);

        //When
        Optional<Long> actualUserId = userService.resolveUserIdForAccess(
                user.getId(),
                user.getRole(),
                NOT_EXISTING_USER_ID
        );

        //Then
        assertThat(actualUserId).isEqualTo(Optional.of(EXISTING_USER_ID));
        verifyNoInteractions(userRepository);
    }

    @Test
    @DisplayName("""
            Test resolveUserIdForAccess() method when user is a MANAGER with existing
             requestUserId.
            """)
    public void resolveUserIdForAccess_ManagerAndExistingRequestUserId_returnsRequestedUserId() {
        //Given
        User user = createTestUser(EXISTING_ID_ANOTHER_USER);
        user.setRole(User.Role.MANAGER);

        when(userRepository.existsSoftDeletedUserById(EXISTING_USER_ID))
                .thenReturn(FALSE_STATUS_FOR_SOFT_DELETED_USER);
        when(userRepository.existsById(EXISTING_USER_ID)).thenReturn(true);

        //When
        Optional<Long> actualUserId = userService.resolveUserIdForAccess(
                user.getId(),
                user.getRole(),
                EXISTING_USER_ID
        );

        //Then
        assertThat(actualUserId).isEqualTo(Optional.of(EXISTING_USER_ID));
        verify(userRepository).existsSoftDeletedUserById(EXISTING_USER_ID);
        verify(userRepository).existsById(EXISTING_USER_ID);
        verifyNoMoreInteractions(userRepository);
    }

    @Test
    @DisplayName("""
            Test resolveUserIdForAccess() method when user is a MANAGER and requestUserId
             is null.
            """)
    public void resolveUserIdForAccess_ManagerAndNullRequestUserId_returnsNull() {
        //Given
        User user = createTestUser(EXISTING_ID_ANOTHER_USER);
        user.setRole(User.Role.MANAGER);

        //When
        Optional<Long> actualUserId = userService.resolveUserIdForAccess(
                user.getId(),
                user.getRole(),
                null
        );

        //Then
        assertThat(actualUserId).isEmpty();
        verifyNoInteractions(userRepository);
    }

    @Test
    @DisplayName("""
            Test resolveUserIdForAccess() method when user is a MANAGER with not existing
             requestUserId.
            """)
    public void resolveUserIdForAccess_ManagerAndNotExistingRequestUserId_ThrowsException() {
        //Given
        User user = createTestUser(EXISTING_ID_ANOTHER_USER);
        user.setRole(User.Role.MANAGER);

        when(userRepository.existsSoftDeletedUserById(NOT_EXISTING_USER_ID))
                .thenReturn(FALSE_STATUS_FOR_SOFT_DELETED_USER);
        when(userRepository.existsById(NOT_EXISTING_USER_ID)).thenReturn(false);

        //When & Then
        assertThatThrownBy(() -> userService.resolveUserIdForAccess(
                user.getId(),
                user.getRole(),
                NOT_EXISTING_USER_ID
        ))
                .isInstanceOf(EntityNotFoundException.class)
                .hasMessage("Can't find user with id: " + NOT_EXISTING_USER_ID);

        verify(userRepository, times(1)).existsSoftDeletedUserById(NOT_EXISTING_USER_ID);
        verify(userRepository, times(1)).existsById(NOT_EXISTING_USER_ID);
        verifyNoMoreInteractions(userRepository);
    }

    @Test
    @DisplayName("""
            Test resolveUserIdForAccess() method when user is a MANAGER with safe deleted
             requestUserId.
            """)
    public void resolveUserIdForAccess_ManagerAndSafeDeletedRequestUserId_ThrowsException() {
        //Given
        User user = createTestUser(EXISTING_ID_ANOTHER_USER);
        user.setRole(User.Role.MANAGER);

        when(userRepository.existsSoftDeletedUserById(SAFE_DELETED_USER_ID))
                .thenReturn(TRUE_STATUS_FOR_SOFT_DELETED_USER);

        //When & Then
        assertThatThrownBy(() -> userService.resolveUserIdForAccess(
                user.getId(),
                user.getRole(),
                SAFE_DELETED_USER_ID
        ))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("User with id: %d was previously deleted."
                        .formatted(SAFE_DELETED_USER_ID));

        verify(userRepository, times(1)).existsSoftDeletedUserById(SAFE_DELETED_USER_ID);
        verifyNoMoreInteractions(userRepository);
    }

    @Test
    @DisplayName("""
    Test canAccessRental() method when user is a MANAGER and ignores requestedUserId.
            """)
    public void canAccessRental_Manager_returnsTrue() {
        //Given
        when(userRepository.existsByIdAndRole(EXISTING_ID_ANOTHER_USER, User.Role.MANAGER))
                .thenReturn(true);

        //When
        boolean actualResult = userService.canAccessRental(EXISTING_ID_ANOTHER_USER,
                EXISTING_USER_ID);

        //Then
        assertThat(actualResult).isTrue();
        verify(userRepository, times(1))
                .existsByIdAndRole(EXISTING_ID_ANOTHER_USER, User.Role.MANAGER);
        verifyNoMoreInteractions(userRepository);
    }

    @Test
    @DisplayName("Test canAccessRental() method when user is a CUSTOMER and has access to rental.")
    public void canAccessRental_Customer_returnsTrue() {
        //Given
        when(userRepository.existsByIdAndRole(EXISTING_USER_ID, User.Role.MANAGER))
                .thenReturn(false);

        //When
        boolean actualResult = userService.canAccessRental(EXISTING_USER_ID,
                EXISTING_USER_ID);

        //Then
        assertThat(actualResult).isTrue();
        verify(userRepository, times(1)).existsByIdAndRole(EXISTING_USER_ID, User.Role.MANAGER);
        verifyNoMoreInteractions(userRepository);
    }

    @Test
    @DisplayName("""
            Test canAccessRental() method when user user is a CUSTOMER and has no access to
             rental.
            """)
    public void canAccessRental_CustomerAndNoAccess_returnsFalse() {
        //Given
        when(userRepository.existsByIdAndRole(EXISTING_USER_ID, User.Role.MANAGER))
                .thenReturn(false);

        //When
        boolean actualResult = userService.canAccessRental(EXISTING_USER_ID,
                EXISTING_ID_ANOTHER_USER);

        //Then
        assertThat(actualResult).isFalse();
        verify(userRepository, times(1)).existsByIdAndRole(EXISTING_USER_ID, User.Role.MANAGER);
        verifyNoMoreInteractions(userRepository);
    }

    @Test
    @DisplayName("Test getEmailByRentalId() method when rental id is exists.")
    public void getEmailByRentalId_ExistingRentalId_ReturnsEmail() {
        //Given
        when(userRepository.getEmailByRentalId(EXISTING_RENTAL_ID))
                .thenReturn(Optional.of(EXISTING_EMAIL));

        //When
        String actualEmail = userService.getEmailByRentalId(EXISTING_RENTAL_ID);

        //Then
        assertThat(actualEmail).isEqualTo(EXISTING_EMAIL);
        verify(userRepository, times(1)).getEmailByRentalId(EXISTING_RENTAL_ID);
        verifyNoMoreInteractions(userRepository);
    }

    @Test
    @DisplayName("Test getEmailByRentalId() method when rental id is not exists.")
    public void getEmailByRentalId_NotExistingRentalId_ThrowsException() {
        //Given
        when(userRepository.getEmailByRentalId(NOT_EXISTING_RENTAL_ID))
                .thenReturn(Optional.empty());

        //When & Then
        assertThatThrownBy(() -> userService.getEmailByRentalId(NOT_EXISTING_RENTAL_ID))
                .isInstanceOf(EntityNotFoundException.class)
                .hasMessage("User email not found with rental id: " + NOT_EXISTING_RENTAL_ID);

        verify(userRepository, times(1)).getEmailByRentalId(NOT_EXISTING_RENTAL_ID);
        verifyNoMoreInteractions(userRepository);
    }
}
