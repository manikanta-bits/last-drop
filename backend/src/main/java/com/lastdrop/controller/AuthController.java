package com.lastdrop.controller;

import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import com.lastdrop.dto.AuthResponse;
import com.lastdrop.dto.LoginRequest;
import com.lastdrop.dto.RegisterRequest;
import com.lastdrop.dto.UserResponse;
import com.lastdrop.security.AuthUser;
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

    public AuthController(AuthService authService) {
        this.authService = authService;
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
