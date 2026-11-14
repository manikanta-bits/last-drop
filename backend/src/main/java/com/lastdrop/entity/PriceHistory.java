package com.lastdrop.entity;

import java.math.BigDecimal;
import java.time.Instant;

import jakarta.persistence.*;

/** One automatic price drop (row of "price_history"). */
@Entity
@Table(name = "price_history")
public class PriceHistory {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "product_id", nullable = false)
    private Product product;

    @Column(name = "old_price", nullable = false, precision = 10, scale = 2) private BigDecimal oldPrice;
    @Column(name = "new_price", nullable = false, precision = 10, scale = 2) private BigDecimal newPrice;
    @Column(name = "changed_at", nullable = false) private Instant changedAt;

    protected PriceHistory() {}

    public PriceHistory(Product product, BigDecimal oldPrice, BigDecimal newPrice, Instant changedAt) {
        this.product = product;
        this.oldPrice = oldPrice;
        this.newPrice = newPrice;
        this.changedAt = changedAt;
    }

    public Long getId() { return id; }
    public BigDecimal getOldPrice() { return oldPrice; }
    public BigDecimal getNewPrice() { return newPrice; }
    public Instant getChangedAt() { return changedAt; }
}
