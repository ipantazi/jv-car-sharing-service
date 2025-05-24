package com.github.ipantazi.carsharing.service.user;

import static com.github.ipantazi.carsharing.util.TestDataUtil.EMAIL_DOMAIN;
import static com.github.ipantazi.carsharing.util.TestDataUtil.EXISTING_USER_ID;
import static com.github.ipantazi.carsharing.util.TestDataUtil.NOT_EXISTING_USER_ID;
import static com.github.ipantazi.carsharing.util.TestDataUtil.createTestUser;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

import com.github.ipantazi.carsharing.exception.EntityNotFoundException;
import com.github.ipantazi.carsharing.model.User;
import com.github.ipantazi.carsharing.repository.user.UserRepository;
import com.github.ipantazi.carsharing.security.CustomUserDetailsService;
import java.util.Optional;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
public class CustomUserDetailsServiceTest {
    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private CustomUserDetailsService customUserDetailsService;

    @Test
    @DisplayName("Should load user by email")
    public void loadUserByUsername_ExistingEmail_ReturnsUserDetails() {
        // Given
        User expectedUser = createTestUser(EXISTING_USER_ID);
        when(userRepository.findByEmail(expectedUser.getEmail()))
                .thenReturn(Optional.of(expectedUser));

        // When
        var actualUserDetails = customUserDetailsService.loadUserByUsername(
                expectedUser.getEmail());

        // Then
        assertThat(actualUserDetails).isNotNull();
        assertThat(actualUserDetails.getUsername()).isEqualTo(expectedUser.getEmail());
        assertThat(actualUserDetails.getPassword()).isEqualTo(expectedUser.getPassword());
        assertThat(actualUserDetails.getAuthorities())
                .isNotEmpty()
                .isEqualTo(expectedUser.getAuthorities());
        assertThat(actualUserDetails.isEnabled()).isTrue();
        verify(userRepository, times(1)).findByEmail(expectedUser.getEmail());
        verifyNoMoreInteractions(userRepository);
    }

    @Test
    @DisplayName("Should throw exception when user not found")
    public void loadUserByUsername_NonExistingEmail_ThrowsException() {
        // Given
        String nonExistingEmail = NOT_EXISTING_USER_ID + EMAIL_DOMAIN;
        when(userRepository.findByEmail(nonExistingEmail))
                .thenReturn(Optional.empty());

        // When & Then
        assertThatThrownBy(() -> customUserDetailsService.loadUserByUsername(nonExistingEmail))
                .isInstanceOf(EntityNotFoundException.class)
                .hasMessageContaining("Can't find user by email: " + nonExistingEmail);
        verify(userRepository, times(1)).findByEmail(nonExistingEmail);
        verifyNoMoreInteractions(userRepository);
    }
}
