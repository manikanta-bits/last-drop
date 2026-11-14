package com.lastdrop.entity;

/** All status/type lists used by the tables. Each matches an ENUM column in MySQL. */
public final class Enums {
    private Enums() {}

    public enum BusinessType { BAKERY, GROCERY, SUPERMARKET, RESTAURANT, DISTRIBUTOR }
    public enum BusinessStatus { PENDING, APPROVED, REJECTED }
    public enum Category { BAKERY, DAIRY, PRODUCE, MEALS, GROCERY, BEVERAGES }
    public enum ProductStatus { ACTIVE, PAUSED, SOLD_OUT, EXPIRED, CANCELLED, REMOVED }
    public enum ReservationStatus { RESERVED, COMPLETED, CANCELLED, EXPIRED }
    public enum NotificationType { PRICE_DROP, NEW_DEAL, RESERVATION, ORDER_STATUS, AUCTION_EXPIRED, ACCOUNT }
}
