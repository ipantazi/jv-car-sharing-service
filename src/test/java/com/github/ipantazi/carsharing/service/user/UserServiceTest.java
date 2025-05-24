package com.github.ipantazi.carsharing.service.user;

import static com.github.ipantazi.carsharing.util.TestDataUtil.EXISTING_USER_ID;
import static com.github.ipantazi.carsharing.util.TestDataUtil.NEW_USER_ID;
import static com.github.ipantazi.carsharing.util.TestDataUtil.USER_DTO_IGNORING_FIELD;
import static com.github.ipantazi.carsharing.util.TestDataUtil.createTestUser;
import static com.github.ipantazi.carsharing.util.TestDataUtil.createTestUserRegistrationRequestDto;
import static com.github.ipantazi.carsharing.util.TestDataUtil.createTestUserRegistrationResponseDto;
import static com.github.ipantazi.carsharing.util.assertions.TestAssertionsUtil.assertObjectsAreEqualIgnoringFields;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

import com.github.ipantazi.carsharing.dto.user.UserRegistrationRequestDto;
import com.github.ipantazi.carsharing.dto.user.UserRegistrationResponseDto;
import com.github.ipantazi.carsharing.exception.RegistrationException;
import com.github.ipantazi.carsharing.mapper.UserMapper;
import com.github.ipantazi.carsharing.model.User;
import com.github.ipantazi.carsharing.repository.user.UserRepository;
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
        UserRegistrationResponseDto expectedUserResponseDto = createTestUserRegistrationResponseDto(
                NEW_USER_ID);
        User user = createTestUser(expectedUserResponseDto);
        UserRegistrationRequestDto userRequestDto = createTestUserRegistrationRequestDto(
                expectedUserResponseDto);
        when(userRepository.existsByEmail(userRequestDto.email())).thenReturn(false);
        when(userMapper.toUserEntity(userRequestDto)).thenReturn(user);
        when(passwordEncoder.encode(userRequestDto.password())).thenReturn(user.getPassword());
        when(userRepository.save(user)).thenReturn(user);
        when(userMapper.toUserDto(user)).thenReturn(expectedUserResponseDto);

        //When
        UserRegistrationResponseDto actualUserResponseDto = userService.register(userRequestDto);

        //Then
        assertObjectsAreEqualIgnoringFields(
                actualUserResponseDto,
                expectedUserResponseDto,
                USER_DTO_IGNORING_FIELD
        );
        verify(userRepository, times(1)).existsByEmail(userRequestDto.email());
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
        UserRegistrationRequestDto userRegistrationRequestDto =
                createTestUserRegistrationRequestDto(EXISTING_USER_ID);
        when(userRepository.existsByEmail(userRegistrationRequestDto.email())).thenReturn(true);

        //When
        assertThatThrownBy(() -> userService.register(userRegistrationRequestDto))
                .isInstanceOf(RegistrationException.class)
                .hasMessageContaining("Can't register user with this email: "
                        + userRegistrationRequestDto.email());
        //Then

        verify(userRepository, times(1)).existsByEmail(userRegistrationRequestDto.email());
        verify(userRepository, never()).save(any(User.class));
        verifyNoMoreInteractions(userRepository);
        verifyNoInteractions(userMapper, passwordEncoder);
    }
}
