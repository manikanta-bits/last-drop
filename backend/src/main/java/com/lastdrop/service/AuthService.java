package com.lastdrop.service;

import java.util.Locale;

import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.lastdrop.dto.AuthResponse;
import com.lastdrop.dto.LoginRequest;
import com.lastdrop.dto.RegisterRequest;
import com.lastdrop.dto.UserResponse;
import com.lastdrop.entity.Role;
import com.lastdrop.entity.User;
import com.lastdrop.exception.ApiException;
import com.lastdrop.repository.UserRepository;
import com.lastdrop.security.JwtService;

/**
 * Registration and login rules.
 *
 * A "service" holds the business logic. Controllers stay thin: they receive
 * the HTTP request, call a service method, and return the result.
 */
@Service
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;

    public AuthService(UserRepository userRepository, PasswordEncoder passwordEncoder, JwtService jwtService) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
    }

    @Transactional
    public AuthResponse register(RegisterRequest request) {
        // Nobody can make themselves an administrator through the public form
        if (request.role() == Role.ADMIN) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Admin accounts cannot be created by registration");
        }

        String email = normalizeEmail(request.email());
        if (userRepository.existsByEmail(email)) {
            throw new ApiException(HttpStatus.CONFLICT, "An account with this email already exists");
        }

        String phone = (request.phone() == null || request.phone().isBlank()) ? null : request.phone().trim();
        User user = new User(
                email,
                passwordEncoder.encode(request.password()),   // store only the BCrypt hash
                request.fullName().trim(),
                phone,
                request.role());
        userRepository.save(user);

        // Log the new user in straight away
        return new AuthResponse(jwtService.createToken(user), UserResponse.from(user));
    }

    @Transactional(readOnly = true)
    public AuthResponse login(LoginRequest request) {
        // Same message for "no such email" and "wrong password", so attackers can't
        // discover which emails are registered.
        User user = userRepository.findByEmail(normalizeEmail(request.email()))
                .filter(u -> passwordEncoder.matches(request.password(), u.getPasswordHash()))
                .orElseThrow(() -> new ApiException(HttpStatus.UNAUTHORIZED, "Incorrect email or password"));

        if (!user.isEnabled()) {
            throw new ApiException(HttpStatus.FORBIDDEN, "This account has been disabled. Contact the administrator.");
        }

        return new AuthResponse(jwtService.createToken(user), UserResponse.from(user));
    }

    @Transactional(readOnly = true)
    public UserResponse getUser(Long userId) {
        return userRepository.findById(userId)
                .map(UserResponse::from)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "User not found"));
    }

    private static String normalizeEmail(String email) {
        return email.trim().toLowerCase(Locale.ROOT);
    }
}
