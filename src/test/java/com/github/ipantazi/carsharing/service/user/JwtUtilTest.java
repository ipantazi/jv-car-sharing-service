package com.github.ipantazi.carsharing.service.user;

import static com.github.ipantazi.carsharing.util.TestDataUtil.EXISTING_EMAIL;
import static com.github.ipantazi.carsharing.util.TestDataUtil.INVALID_TOKEN;
import static com.github.ipantazi.carsharing.util.TestDataUtil.JWT_EXPIRATION_TIME_TEST;
import static com.github.ipantazi.carsharing.util.TestDataUtil.JWT_SECRET_TEST;
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
        jwtUtil = new JwtUtil(JWT_SECRET_TEST);
        ReflectionTestUtils.setField(jwtUtil, "expiration", JWT_EXPIRATION_TIME_TEST);
    }

    @Test
    @DisplayName("Test generateToken method")
    public void generateToken_And_ValidateToken_Success() {
        //When
        String token = jwtUtil.generateToken(EXISTING_EMAIL);

        //Then
        assertThat(jwtUtil.isValidToken(token)).isTrue();
        assertThat(jwtUtil.getUsernameFromToken(token)).isEqualTo(EXISTING_EMAIL);
    }

    @Test
    @DisplayName("Test isValidToken method with invalid token")
    public void isValidToken_InvalidToken_ReturnsThrow() {
        //When & Then
        assertThatThrownBy(() -> jwtUtil.isValidToken(INVALID_TOKEN))
                .isInstanceOf(JwtException.class)
                .hasMessageContaining("Expired or invalid JWT token.");
    }
}
