package com.lastdrop.service;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.Instant;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.stream.Stream;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.lastdrop.dto.Dtos.BusinessSummary;
import com.lastdrop.dto.Dtos.PriceHistoryResponse;
import com.lastdrop.dto.Dtos.ProductRequest;
import com.lastdrop.dto.Dtos.ProductResponse;
import com.lastdrop.dto.Dtos.ProductUpdateRequest;
import com.lastdrop.entity.Business;
import com.lastdrop.entity.Enums.BusinessStatus;
import com.lastdrop.entity.Enums.Category;
import com.lastdrop.entity.Enums.NotificationType;
import com.lastdrop.entity.Enums.ProductStatus;
import com.lastdrop.entity.Enums.ReservationStatus;
import com.lastdrop.entity.Product;
import com.lastdrop.exception.ApiException;
import com.lastdrop.repository.PriceHistoryRepository;
import com.lastdrop.repository.ProductRepository;
import com.lastdrop.repository.ReservationRepository;
import com.lastdrop.util.Geo;
import com.lastdrop.util.Pricing;
import com.lastdrop.websocket.ProductEventPublisher;

@Service
public class ProductService {

    private static final Duration MAX_AUCTION_LENGTH = Duration.ofDays(7);

    private final ProductRepository products;
    private final PriceHistoryRepository history;
    private final ReservationRepository reservations;
    private final BusinessService businessService;
    private final NotificationService notifications;
    private final ProductEventPublisher publisher;
    private final DemoModeService demoMode;

    public ProductService(ProductRepository products, PriceHistoryRepository history, ReservationRepository reservations,
                          BusinessService businessService, NotificationService notifications,
                          ProductEventPublisher publisher, DemoModeService demoMode) {
        this.products = products;
        this.history = history;
        this.reservations = reservations;
        this.businessService = businessService;
        this.notifications = notifications;
        this.publisher = publisher;
        this.demoMode = demoMode;
    }

    /**
     * Live deals with optional filters. If lat/lng are given, distance is calculated and
     * radiusKm (if given) removes shops that are too far away.
     */
    @Transactional(readOnly = true)
    public List<ProductResponse> listLive(String q, Category category, Double lat, Double lng, Double radiusKm,
                                          BigDecimal maxPrice, String sort) {
        String query = q == null ? "" : q.trim().toLowerCase(Locale.ROOT);
        Stream<ProductResponse> stream = products.findLive(Instant.now(), ProductStatus.ACTIVE, BusinessStatus.APPROVED)
                .stream()
                .filter(p -> query.isEmpty() || p.getName().toLowerCase(Locale.ROOT).contains(query)
                        || p.getBusiness().getName().toLowerCase(Locale.ROOT).contains(query))
                .filter(p -> category == null || p.getCategory() == category)
                .filter(p -> maxPrice == null || p.getCurrentPrice().compareTo(maxPrice) <= 0)
                .map(p -> toResponse(p, lat, lng))
                .filter(r -> radiusKm == null || r.distanceKm() == null || r.distanceKm() <= radiusKm);

        Comparator<ProductResponse> order = switch (sort == null ? "" : sort) {
            case "price" -> Comparator.comparing(ProductResponse::currentPrice);
            case "discount" -> Comparator.comparingInt(ProductResponse::discountPercent).reversed();
            case "distance" -> Comparator.comparing(r -> r.distanceKm() == null ? Double.MAX_VALUE : r.distanceKm());
            default -> Comparator.comparing(ProductResponse::expiresAt);   // ending soonest first
        };
        return stream.sorted(order).toList();
    }

    @Transactional(readOnly = true)
    public ProductResponse get(Long id, Double lat, Double lng) {
        Product p = products.findWithBusiness(id)
                .filter(x -> x.getStatus() != ProductStatus.REMOVED)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Product not found"));
        return toResponse(p, lat, lng);
    }

    @Transactional(readOnly = true)
    public List<PriceHistoryResponse> history(Long id) {
        if (!products.existsById(id)) throw new ApiException(HttpStatus.NOT_FOUND, "Product not found");
        return history.findByProductIdOrderByChangedAtAsc(id).stream().map(PriceHistoryResponse::from).toList();
    }

    @Transactional(readOnly = true)
    public List<ProductResponse> mine(Long ownerId) {
        Business b = businessService.requireMine(ownerId);
        return products.findByBusinessIdOrderByCreatedAtDesc(b.getId()).stream().map(p -> toResponse(p, null, null)).toList();
    }

    @Transactional
    public ProductResponse create(Long ownerId, ProductRequest req) {
        Business b = businessService.requireMine(ownerId);
        if (b.getStatus() != BusinessStatus.APPROVED) {
            throw new ApiException(HttpStatus.FORBIDDEN, "Your business must be approved by an admin before you can list products");
        }
        Instant now = Instant.now();
        validatePricing(req, now);

        Product p = new Product(b);
        p.setName(req.name().trim());
        p.setDescription(blankToNull(req.description()));
        p.setCategory(req.category());
        p.setImageUrl(blankToNull(req.imageUrl()));
        p.setQuantityTotal(req.quantity());
        p.setQuantityAvailable(req.quantity());
        p.setOriginalPrice(req.originalPrice());
        p.setStartPrice(req.startPrice());
        p.setMinPrice(req.minPrice());
        p.setCurrentPrice(req.startPrice());
        p.setDropAmount(req.dropAmount());
        p.setDropIntervalMinutes(req.dropIntervalMinutes());
        p.setAuctionStart(now);
        p.setLastDropAt(now);
        p.setExpiresAt(req.expiresAt());
        p.setStatus(ProductStatus.ACTIVE);
        products.save(p);
        publisher.publish(p, "NEW", null);
        return toResponse(p, null, null);
    }

    @Transactional
    public ProductResponse update(Long ownerId, Long id, ProductUpdateRequest req) {
        Product p = requireOwned(ownerId, id);
        requireStatus(p, ProductStatus.ACTIVE, ProductStatus.PAUSED);
        if (req.expiresAt().isAfter(p.getAuctionStart().plus(MAX_AUCTION_LENGTH))) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "An auction can last at most 7 days");
        }
        p.setName(req.name().trim());
        p.setDescription(blankToNull(req.description()));
        p.setCategory(req.category());
        p.setImageUrl(blankToNull(req.imageUrl()));
        p.setExpiresAt(req.expiresAt());
        publisher.publish(p, "STATUS", null);
        return toResponse(p, null, null);
    }

    @Transactional
    public ProductResponse pause(Long ownerId, Long id) {
        Product p = requireOwned(ownerId, id);
        requireStatus(p, ProductStatus.ACTIVE);
        p.setStatus(ProductStatus.PAUSED);
        publisher.publish(p, "STATUS", null);
        return toResponse(p, null, null);
    }

    /** Resuming restarts the drop timer, so the price doesn't fall several times at once. */
    @Transactional
    public ProductResponse resume(Long ownerId, Long id) {
        Product p = requireOwned(ownerId, id);
        requireStatus(p, ProductStatus.PAUSED);
        if (!p.getExpiresAt().isAfter(Instant.now())) {
            throw new ApiException(HttpStatus.CONFLICT, "This auction has already expired");
        }
        p.setStatus(p.getQuantityAvailable() > 0 ? ProductStatus.ACTIVE : ProductStatus.SOLD_OUT);
        p.setLastDropAt(Instant.now());
        publisher.publish(p, "STATUS", null);
        return toResponse(p, null, null);
    }

    /** Seller cancels. Existing reservations stay valid so buyers can still collect. */
    @Transactional
    public ProductResponse cancel(Long ownerId, Long id) {
        Product p = requireOwned(ownerId, id);
        requireStatus(p, ProductStatus.ACTIVE, ProductStatus.PAUSED, ProductStatus.SOLD_OUT);
        p.setStatus(ProductStatus.CANCELLED);
        publisher.publish(p, "STATUS", null);
        return toResponse(p, null, null);
    }

    // ---------------- admin ----------------

    @Transactional(readOnly = true)
    public List<ProductResponse> adminList() {
        return products.findAllWithBusiness().stream().map(p -> toResponse(p, null, null)).toList();
    }

    /** Admin takes down a fraudulent or inappropriate listing; open reservations are cancelled. */
    @Transactional
    public ProductResponse adminRemove(Long id) {
        Product p = products.findWithBusiness(id)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Product not found"));
        p.setStatus(ProductStatus.REMOVED);
        reservations.findByProductIdAndStatus(id, ReservationStatus.RESERVED).forEach(r -> {
            r.setStatus(ReservationStatus.CANCELLED);
            notifications.notify(r.getBuyer().getId(), NotificationType.ORDER_STATUS,
                    "Your reservation for " + p.getName() + " was cancelled because the listing was removed.", p.getId());
        });
        notifications.notify(p.getBusiness().getOwner().getId(), NotificationType.ACCOUNT,
                "Your listing " + p.getName() + " was removed by an administrator.", p.getId());
        publisher.publish(p, "STATUS", null);
        return toResponse(p, null, null);
    }

    // ---------------- helpers ----------------

    public ProductResponse toResponse(Product p, Double lat, Double lng) {
        Business b = p.getBusiness();
        Double km = (lat == null || lng == null) ? null
                : Geo.distanceKm(lat, lng, b.getLatitude().doubleValue(), b.getLongitude().doubleValue());
        return new ProductResponse(p.getId(), p.getName(), p.getDescription(), p.getCategory(), p.getImageUrl(),
                p.getQuantityTotal(), p.getQuantityAvailable(),
                p.getOriginalPrice(), p.getStartPrice(), p.getMinPrice(), p.getCurrentPrice(),
                p.getDropAmount(), p.getDropIntervalMinutes(),
                Pricing.discountPercent(p.getOriginalPrice(), p.getCurrentPrice()),
                p.getAuctionStart(), p.getLastDropAt(), Pricing.nextDropAt(p, demoMode.isEnabled()),
                p.getExpiresAt(), p.getStatus(), p.getVersion(), BusinessSummary.from(b), km);
    }

    private Product requireOwned(Long ownerId, Long productId) {
        Product p = products.findWithBusiness(productId)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Product not found"));
        if (!p.getBusiness().getOwner().getId().equals(ownerId)) {
            throw new ApiException(HttpStatus.FORBIDDEN, "This product belongs to another business");
        }
        return p;
    }

    private static void requireStatus(Product p, ProductStatus... allowed) {
        for (ProductStatus s : allowed) {
            if (p.getStatus() == s) return;
        }
        throw new ApiException(HttpStatus.CONFLICT, "Not possible while the auction is " + p.getStatus());
    }

    private static void validatePricing(ProductRequest r, Instant now) {
        if (r.startPrice().compareTo(r.originalPrice()) > 0) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Starting price cannot be higher than the original price");
        }
        if (r.minPrice().compareTo(r.startPrice()) > 0) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Minimum price cannot be higher than the starting price");
        }
        if (r.expiresAt().isAfter(now.plus(MAX_AUCTION_LENGTH))) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "An auction can last at most 7 days");
        }
    }

    private static String blankToNull(String s) {
        return s == null || s.isBlank() ? null : s.trim();
    }
}
