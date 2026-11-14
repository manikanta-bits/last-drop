package com.lastdrop.service;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.lastdrop.dto.Dtos.AdminUserResponse;
import com.lastdrop.entity.User;
import com.lastdrop.exception.ApiException;
import com.lastdrop.repository.UserRepository;

@Service
public class UserAdminService {

    private final UserRepository users;

    public UserAdminService(UserRepository users) {
        this.users = users;
    }

    @Transactional(readOnly = true)
    public List<AdminUserResponse> list() {
        return users.findAllByOrderByCreatedAtDesc().stream().map(AdminUserResponse::from).toList();
    }

    /** Disabling a user logs them out at once (the token filter checks "enabled" on every request). */
    @Transactional
    public AdminUserResponse setEnabled(Long adminId, Long userId, boolean enabled) {
        if (adminId.equals(userId)) throw new ApiException(HttpStatus.BAD_REQUEST, "You cannot disable your own account");
        User u = users.findById(userId).orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "User not found"));
        u.setEnabled(enabled);
        return AdminUserResponse.from(u);
    }
}
