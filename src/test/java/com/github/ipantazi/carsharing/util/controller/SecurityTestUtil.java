package com.github.ipantazi.carsharing.util.controller;

import static com.github.ipantazi.carsharing.util.TestDataUtil.createTestUser;

import com.github.ipantazi.carsharing.model.User;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;

public class SecurityTestUtil {
    private SecurityTestUtil() {
    }

    public static void setAuthenticationForUser(UserDetails userDetails) {
        Authentication authentication = new UsernamePasswordAuthenticationToken(
                userDetails,
                null,
                userDetails.getAuthorities()
        );
        SecurityContextHolder.getContext().setAuthentication(authentication);
    }

    public static void authenticateTestUser(Long userId, User.Role role) {
        User user = createTestUser(userId);
        user.setRole(role);
        setAuthenticationForUser(user);
    }

}
