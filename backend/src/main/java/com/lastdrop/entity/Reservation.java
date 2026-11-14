package com.lastdrop.entity;

import java.math.BigDecimal;
import java.time.Instant;

import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import com.lastdrop.entity.Enums.ReservationStatus;

import jakarta.persistence.*;

/** A buyer holding some quantity of a product at a locked-in price (Cash on Pickup). */
@Entity
@Table(name = "reservations")
public class Reservation {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "buyer_id", nullable = false)
    private User buyer;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "product_id", nullable = false)
    private Product product;

    @Column(nullable = false) private int quantity;
    @Column(name = "unit_price", nullable = false, precision = 10, scale = 2) private BigDecimal unitPrice;
    @Column(name = "total_price", nullable = false, precision = 10, scale = 2) private BigDecimal totalPrice;
    @Enumerated(EnumType.STRING) @Column(nullable = false) private ReservationStatus status = ReservationStatus.RESERVED;
    @Column(name = "pickup_by", nullable = false) private Instant pickupBy;

    @CreationTimestamp @Column(name = "created_at", nullable = false, updatable = false) private Instant createdAt;
    @UpdateTimestamp @Column(name = "updated_at", nullable = false) private Instant updatedAt;

    protected Reservation() {}

    public Reservation(User buyer, Product product, int quantity, BigDecimal unitPrice, Instant pickupBy) {
        this.buyer = buyer;
        this.product = product;
        this.quantity = quantity;
        this.unitPrice = unitPrice;
        this.totalPrice = unitPrice.multiply(BigDecimal.valueOf(quantity));
        this.pickupBy = pickupBy;
    }

    public Long getId() { return id; }
    public User getBuyer() { return buyer; }
    public Product getProduct() { return product; }
    public int getQuantity() { return quantity; }
    public BigDecimal getUnitPrice() { return unitPrice; }
    public BigDecimal getTotalPrice() { return totalPrice; }
    public ReservationStatus getStatus() { return status; }
    public Instant getPickupBy() { return pickupBy; }
    public Instant getCreatedAt() { return createdAt; }

    public void setStatus(ReservationStatus v) { status = v; }
}
