package com.lastdrop.entity;

import java.time.Instant;

import org.hibernate.annotations.CreationTimestamp;

import com.lastdrop.entity.Enums.NotificationType;

import jakarta.persistence.*;

/** An in-app message for one user. */
@Entity
@Table(name = "notifications")
public class Notification {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id", nullable = false) private Long userId;
    @Enumerated(EnumType.STRING) @Column(nullable = false) private NotificationType type;
    @Column(nullable = false, length = 300) private String message;
    @Column(name = "product_id") private Long productId;
    @Column(name = "is_read", nullable = false) private boolean read;
    @CreationTimestamp @Column(name = "created_at", nullable = false, updatable = false) private Instant createdAt;

    protected Notification() {}

    public Notification(Long userId, NotificationType type, String message, Long productId) {
        this.userId = userId;
        this.type = type;
        this.message = message.length() > 300 ? message.substring(0, 300) : message;
        this.productId = productId;
    }

    public Long getId() { return id; }
    public Long getUserId() { return userId; }
    public NotificationType getType() { return type; }
    public String getMessage() { return message; }
    public Long getProductId() { return productId; }
    public boolean isRead() { return read; }
    public Instant getCreatedAt() { return createdAt; }

    public void setRead(boolean v) { read = v; }
}
