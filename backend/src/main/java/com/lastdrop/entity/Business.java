package com.lastdrop.entity;

import java.math.BigDecimal;
import java.time.Instant;

import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import com.lastdrop.entity.Enums.BusinessStatus;
import com.lastdrop.entity.Enums.BusinessType;

import jakarta.persistence.*;

/** A shop (row of "businesses"). Must be APPROVED by an admin before it can list products. */
@Entity
@Table(name = "businesses")
public class Business {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "owner_id", nullable = false, unique = true)
    private User owner;

    @Column(nullable = false, length = 120) private String name;
    @Enumerated(EnumType.STRING) @Column(name = "business_type", nullable = false) private BusinessType businessType;
    @Column(nullable = false) private String address;
    @Column(length = 20) private String phone;
    @Column(nullable = false, precision = 9, scale = 6) private BigDecimal latitude;
    @Column(nullable = false, precision = 9, scale = 6) private BigDecimal longitude;
    @Enumerated(EnumType.STRING) @Column(nullable = false) private BusinessStatus status = BusinessStatus.PENDING;

    @CreationTimestamp @Column(name = "created_at", nullable = false, updatable = false) private Instant createdAt;
    @UpdateTimestamp @Column(name = "updated_at", nullable = false) private Instant updatedAt;

    protected Business() {}

    public Business(User owner) { this.owner = owner; }

    public Long getId() { return id; }
    public User getOwner() { return owner; }
    public String getName() { return name; }
    public BusinessType getBusinessType() { return businessType; }
    public String getAddress() { return address; }
    public String getPhone() { return phone; }
    public BigDecimal getLatitude() { return latitude; }
    public BigDecimal getLongitude() { return longitude; }
    public BusinessStatus getStatus() { return status; }
    public Instant getCreatedAt() { return createdAt; }

    public void setName(String v) { name = v; }
    public void setBusinessType(BusinessType v) { businessType = v; }
    public void setAddress(String v) { address = v; }
    public void setPhone(String v) { phone = v; }
    public void setLatitude(BigDecimal v) { latitude = v; }
    public void setLongitude(BigDecimal v) { longitude = v; }
    public void setStatus(BusinessStatus v) { status = v; }
}
