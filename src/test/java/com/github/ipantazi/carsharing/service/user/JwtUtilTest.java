package com.github.ipantazi.carsharing.service.user;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.github.ipantazi.carsharing.security.JwtUtil;
import io.jsonwebtoken.JwtException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

public class JwtUtilTest {
    private JwtUtil jwtUtil;

    @BeforeEach
    void setUp() {
        jwtUtil = new JwtUtil("testsecretkeytestsecretkeytest12");
        ReflectionTestUtils.setField(jwtUtil, "expiration", 3600000L);
    }

    @Test
    @DisplayName("Test generateToken method")
    public void generateToken_And_ValidateToken_Success() {
        //Given
        String testEmail = "test@example.com";

        //When
        String token = jwtUtil.generateToken(testEmail);

        //Then
        assertThat(jwtUtil.isValidToken(token)).isTrue();
        assertThat(jwtUtil.getUsernameFromToken(token)).isEqualTo(testEmail);
    }

    @Test
    @DisplayName("Test isValidToken method with invalid token")
    public void isValidToken_InvalidToken_ReturnsThrow() {
        //Given
        String invalidToken = "invalidtoken";

        //When & Then
        assertThatThrownBy(() -> jwtUtil.isValidToken(invalidToken))
                .isInstanceOf(JwtException.class)
                .hasMessageContaining("Expired or invalid JWT token.");
    }
}
