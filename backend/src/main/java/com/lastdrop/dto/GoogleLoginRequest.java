package com.lastdrop.dto;

import com.lastdrop.entity.Role;

import jakarta.validation.constraints.NotBlank;

/**
 * credential = the ID token Google gives the browser.
 * role = account type for a NEW account (BUYER if empty). Ignored for existing accounts.
 */
public record GoogleLoginRequest(@NotBlank(message = "Missing Google credential") String credential, Role role) {
}
