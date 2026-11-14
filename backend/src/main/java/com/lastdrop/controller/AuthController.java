package com.lastdrop.controller;

import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

import com.lastdrop.dto.AuthResponse;
import com.lastdrop.dto.GoogleLoginRequest;
import com.lastdrop.dto.LoginRequest;
import com.lastdrop.dto.RegisterRequest;
import com.lastdrop.dto.UserResponse;
import com.lastdrop.security.AuthUser;
import com.lastdrop.security.GoogleTokenVerifier;
import com.lastdrop.service.AuthService;

import jakarta.validation.Valid;

/**
 * POST /api/auth/register  - create an account (public)     -> 201 + token
 * POST /api/auth/login     - log in (public)                -> 200 + token
 * GET  /api/auth/me        - who am I? (needs a token)      -> 200 + user
 */
@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final AuthService authService;
    private final GoogleTokenVerifier googleVerifier;

    public AuthController(AuthService authService, GoogleTokenVerifier googleVerifier) {
        this.authService = authService;
        this.googleVerifier = googleVerifier;
    }

    /** Public settings the login page needs (the Google client id is public by design). */
    @GetMapping("/config")
    public Map<String, Object> config() {
        return Map.of("googleEnabled", googleVerifier.isConfigured(), "googleClientId", googleVerifier.getClientId());
    }

    /** POST /api/auth/google - sign in or sign up with a Google ID token. */
    @PostMapping("/google")
    public AuthResponse google(@Valid @RequestBody GoogleLoginRequest request) {
        return authService.googleLogin(request);
    }

    @PostMapping("/register")
    @ResponseStatus(HttpStatus.CREATED)
    public AuthResponse register(@Valid @RequestBody RegisterRequest request) {
        return authService.register(request);
    }

    @PostMapping("/login")
    public AuthResponse login(@Valid @RequestBody LoginRequest request) {
        return authService.login(request);
    }

    @GetMapping("/me")
    public UserResponse me(@AuthenticationPrincipal AuthUser me) {
        return authService.getUser(me.id());
    }
}
