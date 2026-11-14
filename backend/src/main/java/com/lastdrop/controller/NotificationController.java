package com.lastdrop.controller;

import java.util.List;
import java.util.Map;

import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import com.lastdrop.dto.Dtos.NotificationResponse;
import com.lastdrop.security.AuthUser;
import com.lastdrop.service.NotificationService;

/** Any logged-in user: their own notifications only. */
@RestController
@RequestMapping("/api/notifications")
public class NotificationController {

    private final NotificationService service;

    public NotificationController(NotificationService service) {
        this.service = service;
    }

    @GetMapping
    public List<NotificationResponse> list(@AuthenticationPrincipal AuthUser me) {
        return service.list(me.id());
    }

    @GetMapping("/unread-count")
    public Map<String, Long> unread(@AuthenticationPrincipal AuthUser me) {
        return Map.of("count", service.unreadCount(me.id()));
    }

    @PutMapping("/{id}/read")
    public NotificationResponse read(@AuthenticationPrincipal AuthUser me, @PathVariable Long id) {
        return service.markRead(me.id(), id);
    }

    @PutMapping("/read-all")
    public Map<String, Integer> readAll(@AuthenticationPrincipal AuthUser me) {
        return Map.of("updated", service.markAllRead(me.id()));
    }
}
