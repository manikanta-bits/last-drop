package com.lastdrop.service;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.lastdrop.dto.Dtos.NotificationResponse;
import com.lastdrop.entity.Enums.NotificationType;
import com.lastdrop.entity.Notification;
import com.lastdrop.entity.Role;
import com.lastdrop.exception.ApiException;
import com.lastdrop.repository.NotificationRepository;
import com.lastdrop.repository.UserRepository;

@Service
public class NotificationService {

    private final NotificationRepository notifications;
    private final UserRepository users;

    public NotificationService(NotificationRepository notifications, UserRepository users) {
        this.notifications = notifications;
        this.users = users;
    }

    @Transactional
    public void notify(Long userId, NotificationType type, String message, Long productId) {
        notifications.save(new Notification(userId, type, message, productId));
    }

    @Transactional
    public void notifyAdmins(NotificationType type, String message) {
        users.findByRole(Role.ADMIN).forEach(admin -> notify(admin.getId(), type, message, null));
    }

    @Transactional(readOnly = true)
    public List<NotificationResponse> list(Long userId) {
        return notifications.findTop50ByUserIdOrderByCreatedAtDescIdDesc(userId).stream()
                .map(NotificationResponse::from).toList();
    }

    @Transactional(readOnly = true)
    public long unreadCount(Long userId) {
        return notifications.countByUserIdAndReadFalse(userId);
    }

    @Transactional
    public NotificationResponse markRead(Long userId, Long id) {
        Notification n = notifications.findById(id)
                .filter(x -> x.getUserId().equals(userId))
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Notification not found"));
        n.setRead(true);
        return NotificationResponse.from(n);
    }

    @Transactional
    public int markAllRead(Long userId) {
        return notifications.markAllRead(userId);
    }
}
