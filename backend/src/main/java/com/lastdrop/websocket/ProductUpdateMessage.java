package com.lastdrop.websocket;

import java.math.BigDecimal;
import java.time.Instant;

import com.lastdrop.entity.Enums.ProductStatus;

/**
 * What browsers receive when a product changes.
 * type: PRICE_DROP, STOCK, STATUS or NEW.
 * version always increases, so browsers can ignore old or duplicate messages.
 */
public record ProductUpdateMessage(Long productId, String type, String name, String businessName,
                                   BigDecimal oldPrice, BigDecimal currentPrice, BigDecimal originalPrice,
                                   BigDecimal minPrice, int discountPercent, int quantityAvailable,
                                   ProductStatus status, Instant lastDropAt, Instant nextDropAt,
                                   Instant expiresAt, long version, Instant sentAt) {
}
