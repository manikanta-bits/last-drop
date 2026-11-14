package com.lastdrop.dto;

import jakarta.validation.constraints.NotBlank;

/** The JSON the login form sends. */
public record LoginRequest(

        @NotBlank(message = "Email is required")
        String email,

        @NotBlank(message = "Password is required")
        String password
) {
}
