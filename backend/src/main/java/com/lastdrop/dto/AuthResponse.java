package com.lastdrop.dto;

/**
 * Sent back after a successful login or registration.
 * The frontend stores "token" and sends it with every later request.
 */
public record AuthResponse(String token, UserResponse user) {
}
