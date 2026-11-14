package com.lastdrop.entity;

import java.math.BigDecimal;
import java.time.Instant;

import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import com.lastdrop.entity.Enums.Category;
import com.lastdrop.entity.Enums.ProductStatus;

import jakarta.persistence.*;

/**
 * A product on sale. Each product is its own Dutch auction.
 * currentPrice and lastDropAt are changed ONLY by AuctionService.
 * "version" makes Hibernate refuse to save stale copies (optimistic locking).
 */
@Entity
@Table(name = "products")
public class Product {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "business_id", nullable = false)
    private Business business;

    @Column(nullable = false, length = 120) private String name;
    @Column(length = 500) private String description;
    @Enumerated(EnumType.STRING) @Column(nullable = false) private Category category;
    @Column(name = "image_url", length = 500) private String imageUrl;

    @Column(name = "quantity_total", nullable = false) private int quantityTotal;
    @Column(name = "quantity_available", nullable = false) private int quantityAvailable;

    @Column(name = "original_price", nullable = false, precision = 10, scale = 2) private BigDecimal originalPrice;
    @Column(name = "start_price", nullable = false, precision = 10, scale = 2) private BigDecimal startPrice;
    @Column(name = "min_price", nullable = false, precision = 10, scale = 2) private BigDecimal minPrice;
    @Column(name = "current_price", nullable = false, precision = 10, scale = 2) private BigDecimal currentPrice;
    @Column(name = "drop_amount", nullable = false, precision = 10, scale = 2) private BigDecimal dropAmount;
    @Column(name = "drop_interval_minutes", nullable = false) private int dropIntervalMinutes;

    @Column(name = "auction_start", nullable = false) private Instant auctionStart;
    @Column(name = "last_drop_at", nullable = false) private Instant lastDropAt;
    @Column(name = "expires_at", nullable = false) private Instant expiresAt;
    @Enumerated(EnumType.STRING) @Column(nullable = false) private ProductStatus status = ProductStatus.ACTIVE;

    @Version @Column(nullable = false) private long version;
    @CreationTimestamp @Column(name = "created_at", nullable = false, updatable = false) private Instant createdAt;
    @UpdateTimestamp @Column(name = "updated_at", nullable = false) private Instant updatedAt;

    protected Product() {}

    public Product(Business business) { this.business = business; }

    public Long getId() { return id; }
    public Business getBusiness() { return business; }
    public String getName() { return name; }
    public String getDescription() { return description; }
    public Category getCategory() { return category; }
    public String getImageUrl() { return imageUrl; }
    public int getQuantityTotal() { return quantityTotal; }
    public int getQuantityAvailable() { return quantityAvailable; }
    public BigDecimal getOriginalPrice() { return originalPrice; }
    public BigDecimal getStartPrice() { return startPrice; }
    public BigDecimal getMinPrice() { return minPrice; }
    public BigDecimal getCurrentPrice() { return currentPrice; }
    public BigDecimal getDropAmount() { return dropAmount; }
    public int getDropIntervalMinutes() { return dropIntervalMinutes; }
    public Instant getAuctionStart() { return auctionStart; }
    public Instant getLastDropAt() { return lastDropAt; }
    public Instant getExpiresAt() { return expiresAt; }
    public ProductStatus getStatus() { return status; }
    public long getVersion() { return version; }
    public Instant getCreatedAt() { return createdAt; }

    public void setName(String v) { name = v; }
    public void setDescription(String v) { description = v; }
    public void setCategory(Category v) { category = v; }
    public void setImageUrl(String v) { imageUrl = v; }
    public void setQuantityTotal(int v) { quantityTotal = v; }
    public void setQuantityAvailable(int v) { quantityAvailable = v; }
    public void setOriginalPrice(BigDecimal v) { originalPrice = v; }
    public void setStartPrice(BigDecimal v) { startPrice = v; }
    public void setMinPrice(BigDecimal v) { minPrice = v; }
    public void setCurrentPrice(BigDecimal v) { currentPrice = v; }
    public void setDropAmount(BigDecimal v) { dropAmount = v; }
    public void setDropIntervalMinutes(int v) { dropIntervalMinutes = v; }
    public void setAuctionStart(Instant v) { auctionStart = v; }
    public void setLastDropAt(Instant v) { lastDropAt = v; }
    public void setExpiresAt(Instant v) { expiresAt = v; }
    public void setStatus(ProductStatus v) { status = v; }
}
