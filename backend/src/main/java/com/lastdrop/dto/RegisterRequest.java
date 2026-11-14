package com.lastdrop.dto;

import com.lastdrop.entity.Role;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/**
 * The JSON the registration form sends.
 * A DTO (Data Transfer Object) is a simple container for data going in or out of the API.
 * The annotations are validation rules; Spring rejects the request with 400 if any fail.
 */
public record RegisterRequest(

        @NotBlank(message = "Email is required")
        @Email(message = "Enter a valid email address")
        @Size(max = 120, message = "Email is too long")
        String email,

        @NotBlank(message = "Password is required")
        @Size(min = 8, max = 72, message = "Password must be 8 to 72 characters")
        @Pattern(regexp = "^(?=.*[A-Za-z])(?=.*\\d).+$",
                 message = "Password must contain at least one letter and one number")
        String password,

        @NotBlank(message = "Name is required")
        @Size(max = 100, message = "Name is too long")
        String fullName,

        @Pattern(regexp = "^$|^[0-9+\\- ]{7,20}$", message = "Enter a valid phone number")
        String phone,

        @NotNull(message = "Choose an account type")
        Role role
) {
}
