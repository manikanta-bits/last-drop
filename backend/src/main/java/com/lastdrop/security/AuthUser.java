package com.lastdrop.security;

import com.lastdrop.entity.Role;

/**
 * The logged-in user for the current request.
 *
 * Controllers get it with:  @AuthenticationPrincipal AuthUser me
 */
public record AuthUser(Long id, String email, String fullName, Role role) {
}
