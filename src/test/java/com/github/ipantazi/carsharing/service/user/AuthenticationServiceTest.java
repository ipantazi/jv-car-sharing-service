package com.github.ipantazi.carsharing.service.user;

import static com.github.ipantazi.carsharing.util.TestDataUtil.EXISTING_USER_ID;
import static com.github.ipantazi.carsharing.util.TestDataUtil.NOT_EXISTING_USER_ID;
import static com.github.ipantazi.carsharing.util.TestDataUtil.createTestUserLoginRequestDto;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

import com.github.ipantazi.carsharing.dto.user.UserLoginRequestDto;
import com.github.ipantazi.carsharing.dto.user.UserLoginResponseDto;
import com.github.ipantazi.carsharing.security.AuthenticationService;
import com.github.ipantazi.carsharing.security.JwtUtil;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.core.Authentication;

@ExtendWith(MockitoExtension.class)
public class AuthenticationServiceTest {
    @Mock
    private AuthenticationManager authenticationManager;

    @Mock
    private JwtUtil jwtUtil;

    @InjectMocks
    private AuthenticationService authenticationService;

    @Test
    @DisplayName("When valid credentials are provided, "
            + "then the authentication service should return a valid token.")
    public void authenticate_ValidCredentials_ReturnsToken() {
        //Given
        String expectedToken = "Test token";
        UserLoginRequestDto requestDto = createTestUserLoginRequestDto(EXISTING_USER_ID);
        Authentication authentication = mock(Authentication.class);

        when(authenticationManager.authenticate(any())).thenReturn(authentication);
        when(authentication.getName()).thenReturn(requestDto.email());
        when(jwtUtil.generateToken(requestDto.email())).thenReturn(expectedToken);

        //When
        UserLoginResponseDto actualResponseDto = authenticationService.authenticate(requestDto);

        //Then
        assertThat(actualResponseDto.token()).isEqualTo(expectedToken);
        verify(authenticationManager, times(1)).authenticate(any());
        verify(jwtUtil, times(1)).generateToken(requestDto.email());
        verifyNoMoreInteractions(authenticationManager, jwtUtil);
    }

    @Test
    @DisplayName("When invalid credentials are provided, "
            + "then the authentication service should throw an error.")
    public void authenticate_InvalidCredentials_ReturnError() {
        //Given
        UserLoginRequestDto requestDto = createTestUserLoginRequestDto(NOT_EXISTING_USER_ID);
        when(authenticationManager.authenticate(any()))
                .thenThrow(new RuntimeException("Invalid credentials"));

        //When & Then
        assertThatThrownBy(() -> authenticationService.authenticate(requestDto))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("Invalid credentials");
        verify(authenticationManager, times(1)).authenticate(any());
        verifyNoMoreInteractions(authenticationManager);
        verifyNoInteractions(jwtUtil);
    }
}
