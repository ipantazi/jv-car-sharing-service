package com.github.ipantazi.carsharing.util.controller;

import static com.github.ipantazi.carsharing.util.TestDataUtil.createTestUser;

import com.github.ipantazi.carsharing.model.User;
import com.github.ipantazi.carsharing.security.CustomUserDetails;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;

public class SecurityTestUtil {
    private SecurityTestUtil() {
    }

    public static CustomUserDetails wrapAsUserDetails(User user) {
        return new CustomUserDetails(
                user.getId(),
                user.getEmail(),
                user.getPassword(),
                user.getAuthorities(),
                user.getRole(),
                user.isEnabled()
        );
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
        CustomUserDetails customUserDetails = wrapAsUserDetails(user);
        setAuthenticationForUser(customUserDetails);
    }

}
