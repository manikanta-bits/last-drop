package com.lastdrop.dto;

import com.lastdrop.entity.Role;
import com.lastdrop.entity.User;

/**
 * What the API sends back about a user. Note: the password hash is never included.
 */
public record UserResponse(Long id, String email, String fullName, String phone, Role role) {

    public static UserResponse from(User user) {
        return new UserResponse(user.getId(), user.getEmail(), user.getFullName(), user.getPhone(), user.getRole());
    }
}
