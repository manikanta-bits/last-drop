package com.lastdrop.dto;

import java.math.BigDecimal;
import java.time.Instant;

import com.lastdrop.entity.Business;
import com.lastdrop.entity.Enums.BusinessStatus;
import com.lastdrop.entity.Enums.BusinessType;
import com.lastdrop.entity.Enums.Category;
import com.lastdrop.entity.Enums.NotificationType;
import com.lastdrop.entity.Enums.ProductStatus;
import com.lastdrop.entity.Enums.ReservationStatus;
import com.lastdrop.entity.Notification;
import com.lastdrop.entity.PriceHistory;
import com.lastdrop.entity.Reservation;
import com.lastdrop.entity.Role;
import com.lastdrop.entity.User;

import jakarta.validation.constraints.*;

/** Data sent to and from the API. Requests are validated; responses never expose passwords. */
public final class Dtos {

    private Dtos() {}

    // ---------------- Businesses ----------------

    public record BusinessRequest(
            @NotBlank(message = "Shop name is required") @Size(max = 120) String name,
            @NotNull(message = "Choose a business type") BusinessType businessType,
            @NotBlank(message = "Address is required") @Size(max = 255) String address,
            @Pattern(regexp = "^$|^[0-9+\\- ]{7,20}$", message = "Enter a valid phone number") String phone,
            @NotNull(message = "Pick the shop location on the map") @DecimalMin("-90") @DecimalMax("90") BigDecimal latitude,
            @NotNull(message = "Pick the shop location on the map") @DecimalMin("-180") @DecimalMax("180") BigDecimal longitude) {
    }

    public record BusinessResponse(Long id, String name, BusinessType businessType, String address, String phone,
                                   BigDecimal latitude, BigDecimal longitude, BusinessStatus status,
                                   String ownerName, String ownerEmail, Instant createdAt) {
        public static BusinessResponse from(Business b) {
            return new BusinessResponse(b.getId(), b.getName(), b.getBusinessType(), b.getAddress(), b.getPhone(),
                    b.getLatitude(), b.getLongitude(), b.getStatus(),
                    b.getOwner().getFullName(), b.getOwner().getEmail(), b.getCreatedAt());
        }
    }

    public record NearbyBusinessResponse(Long id, String name, BusinessType businessType, String address,
                                         BigDecimal latitude, BigDecimal longitude, double distanceKm, long liveDeals) {
    }

    /** The shop details shown on every product card. */
    public record BusinessSummary(Long id, String name, BusinessType businessType, String address, String phone,
                                  BigDecimal latitude, BigDecimal longitude) {
        public static BusinessSummary from(Business b) {
            return new BusinessSummary(b.getId(), b.getName(), b.getBusinessType(), b.getAddress(), b.getPhone(),
                    b.getLatitude(), b.getLongitude());
        }
    }

    // ---------------- Products / auctions ----------------

    public record ProductRequest(
            @NotBlank(message = "Product name is required") @Size(max = 120) String name,
            @Size(max = 500) String description,
            @NotNull(message = "Choose a category") Category category,
            @Size(max = 500) @Pattern(regexp = "^$|^https?://.+", message = "Image must be a web link (http/https)") String imageUrl,
            @NotNull(message = "Required") @Min(value = 1, message = "At least 1") @Max(value = 1000, message = "At most 1000") Integer quantity,
            @NotNull(message = "Required") @DecimalMin(value = "1.00", message = "Must be at least 1") @Digits(integer = 8, fraction = 2) BigDecimal originalPrice,
            @NotNull(message = "Required") @DecimalMin(value = "1.00", message = "Must be at least 1") @Digits(integer = 8, fraction = 2) BigDecimal startPrice,
            @NotNull(message = "Required") @DecimalMin(value = "1.00", message = "Must be at least 1") @Digits(integer = 8, fraction = 2) BigDecimal minPrice,
            @NotNull(message = "Required") @DecimalMin(value = "0.50", message = "Must be at least 0.50") @Digits(integer = 8, fraction = 2) BigDecimal dropAmount,
            @NotNull(message = "Required") @Min(value = 1, message = "At least 1 minute") @Max(value = 1440, message = "At most 24 hours") Integer dropIntervalMinutes,
            @NotNull(message = "Expiry time is required") @Future(message = "Expiry must be in the future") Instant expiresAt) {
    }

    public record ProductUpdateRequest(
            @NotBlank(message = "Product name is required") @Size(max = 120) String name,
            @Size(max = 500) String description,
            @NotNull(message = "Choose a category") Category category,
            @Size(max = 500) @Pattern(regexp = "^$|^https?://.+", message = "Image must be a web link (http/https)") String imageUrl,
            @NotNull(message = "Expiry time is required") @Future(message = "Expiry must be in the future") Instant expiresAt) {
    }

    public record ProductResponse(Long id, String name, String description, Category category, String imageUrl,
                                  int quantityTotal, int quantityAvailable,
                                  BigDecimal originalPrice, BigDecimal startPrice, BigDecimal minPrice,
                                  BigDecimal currentPrice, BigDecimal dropAmount, int dropIntervalMinutes,
                                  int discountPercent, Instant auctionStart, Instant lastDropAt, Instant nextDropAt,
                                  Instant expiresAt, ProductStatus status, long version,
                                  BusinessSummary business, Double distanceKm) {
    }

    public record PriceHistoryResponse(BigDecimal oldPrice, BigDecimal newPrice, Instant changedAt) {
        public static PriceHistoryResponse from(PriceHistory h) {
            return new PriceHistoryResponse(h.getOldPrice(), h.getNewPrice(), h.getChangedAt());
        }
    }

    // ---------------- Reservations ----------------

    public record ReservationRequest(
            @NotNull(message = "Product is required") Long productId,
            @NotNull(message = "Quantity is required") @Min(value = 1, message = "At least 1") @Max(value = 100, message = "At most 100") Integer quantity) {
    }

    public record ReservationResponse(Long id, int quantity, BigDecimal unitPrice, BigDecimal totalPrice,
                                      BigDecimal originalUnitPrice, BigDecimal savings, ReservationStatus status,
                                      Instant pickupBy, Instant createdAt,
                                      Long productId, String productName, String businessName, String businessAddress,
                                      String businessPhone, String buyerName, String buyerEmail, Role buyerRole) {
        public static ReservationResponse from(Reservation r) {
            var p = r.getProduct();
            var b = p.getBusiness();
            var u = r.getBuyer();
            BigDecimal savings = p.getOriginalPrice().subtract(r.getUnitPrice()).multiply(BigDecimal.valueOf(r.getQuantity()));
            return new ReservationResponse(r.getId(), r.getQuantity(), r.getUnitPrice(), r.getTotalPrice(),
                    p.getOriginalPrice(), savings, r.getStatus(), r.getPickupBy(), r.getCreatedAt(),
                    p.getId(), p.getName(), b.getName(), b.getAddress(), b.getPhone(),
                    u.getFullName(), u.getEmail(), u.getRole());
        }
    }

    // ---------------- Notifications ----------------

    public record NotificationResponse(Long id, NotificationType type, String message, Long productId,
                                       boolean read, Instant createdAt) {
        public static NotificationResponse from(Notification n) {
            return new NotificationResponse(n.getId(), n.getType(), n.getMessage(), n.getProductId(), n.isRead(), n.getCreatedAt());
        }
    }

    // ---------------- Admin ----------------

    public record AdminUserResponse(Long id, String email, String fullName, String phone, Role role,
                                    boolean enabled, Instant createdAt) {
        public static AdminUserResponse from(User u) {
            return new AdminUserResponse(u.getId(), u.getEmail(), u.getFullName(), u.getPhone(), u.getRole(),
                    u.isEnabled(), u.getCreatedAt());
        }
    }

    public record EnabledRequest(@NotNull Boolean enabled) {
    }

    public record DemoModeResponse(boolean enabled, long dropEverySeconds) {
    }

    // ---------------- Dashboards ----------------

    /** Environmental numbers are ESTIMATES; "assumptions" explains how they are calculated. */
    public record Impact(long itemsRescued, double estimatedFoodKg, double estimatedCo2Kg, String assumptions) {
    }

    public record AdminDashboard(long totalUsers, long buyers, long ngos, long businessUsers,
                                 long approvedBusinesses, long pendingBusinesses,
                                 long totalProducts, long activeAuctions, long soldOut, long expired,
                                 long activeReservations, long completedReservations,
                                 BigDecimal estimatedBuyerSavings, Impact impact, boolean demoMode) {
    }

    public record BusinessDashboard(BusinessStatus businessStatus, long activeListings, long totalInventoryListed,
                                    long productsSold, BigDecimal revenueRecovered, long activeReservations) {
    }

    public record BuyerDashboard(long activeReservations, long completedReservations, BigDecimal totalSavings,
                                 Impact impact) {
    }
}
