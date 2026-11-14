package com.lastdrop.service;

import java.time.Duration;
import java.time.Instant;
import java.util.List;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;

import com.lastdrop.dto.Dtos.ReservationRequest;
import com.lastdrop.dto.Dtos.ReservationResponse;
import com.lastdrop.entity.Business;
import com.lastdrop.entity.Enums.NotificationType;
import com.lastdrop.entity.Enums.ProductStatus;
import com.lastdrop.entity.Enums.ReservationStatus;
import com.lastdrop.entity.Product;
import com.lastdrop.entity.Reservation;
import com.lastdrop.entity.User;
import com.lastdrop.exception.ApiException;
import com.lastdrop.repository.ProductRepository;
import com.lastdrop.repository.ReservationRepository;
import com.lastdrop.repository.UserRepository;
import com.lastdrop.websocket.ProductEventPublisher;

@Service
public class ReservationService {

    private final ReservationRepository reservations;
    private final ProductRepository products;
    private final UserRepository users;
    private final BusinessService businessService;
    private final NotificationService notifications;
    private final ProductEventPublisher publisher;
    private final TransactionTemplate tx;
    private final Duration holdTime;

    public ReservationService(ReservationRepository reservations, ProductRepository products, UserRepository users,
                              BusinessService businessService, NotificationService notifications,
                              ProductEventPublisher publisher, TransactionTemplate tx,
                              @Value("${app.reservation.hold-minutes:30}") long holdMinutes) {
        this.reservations = reservations;
        this.products = products;
        this.users = users;
        this.businessService = businessService;
        this.notifications = notifications;
        this.publisher = publisher;
        this.tx = tx;
        this.holdTime = Duration.ofMinutes(holdMinutes);
    }

    /**
     * Reserve at the current server price.
     * The product row is LOCKED while we check and reduce stock, so two buyers
     * can never both get the last item (no overselling).
     */
    @Transactional
    public ReservationResponse reserve(Long buyerId, ReservationRequest req) {
        Instant now = Instant.now();
        Product p = products.findByIdForUpdate(req.productId())
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Product not found"));

        if (p.getStatus() != ProductStatus.ACTIVE) {
            throw new ApiException(HttpStatus.CONFLICT, switch (p.getStatus()) {
                case SOLD_OUT -> "Sorry, this product is sold out";
                case EXPIRED -> "Sorry, this deal has expired";
                case PAUSED -> "The seller has paused this auction";
                default -> "This auction is no longer available";
            });
        }
        if (!p.getExpiresAt().isAfter(now)) {
            throw new ApiException(HttpStatus.CONFLICT, "Sorry, this deal has expired");
        }
        if (req.quantity() > p.getQuantityAvailable()) {
            throw new ApiException(HttpStatus.CONFLICT, "Only " + p.getQuantityAvailable() + " left");
        }

        User buyer = users.getReferenceById(buyerId);
        Instant pickupBy = now.plus(holdTime).isBefore(p.getExpiresAt()) ? now.plus(holdTime) : p.getExpiresAt();
        Reservation r = new Reservation(buyer, p, req.quantity(), p.getCurrentPrice(), pickupBy);
        reservations.save(r);

        p.setQuantityAvailable(p.getQuantityAvailable() - req.quantity());
        if (p.getQuantityAvailable() == 0) p.setStatus(ProductStatus.SOLD_OUT);
        products.saveAndFlush(p);

        Business shop = p.getBusiness();
        notifications.notify(buyerId, NotificationType.RESERVATION,
                "Reservation confirmed: " + r.getQuantity() + " x " + p.getName() + " at " + shop.getName()
                        + ". Pay ₹" + r.getTotalPrice() + " at pickup.", p.getId());
        notifications.notify(shop.getOwner().getId(), NotificationType.RESERVATION,
                r.getBuyer().getFullName() + " reserved " + r.getQuantity() + " x " + p.getName() + ".", p.getId());
        publisher.publish(p, "STOCK", null);
        return ReservationResponse.from(r);
    }

    @Transactional(readOnly = true)
    public List<ReservationResponse> mine(Long buyerId) {
        return reservations.findForBuyer(buyerId).stream().map(ReservationResponse::from).toList();
    }

    @Transactional(readOnly = true)
    public List<ReservationResponse> forMyBusiness(Long ownerId) {
        Business b = businessService.requireMine(ownerId);
        return reservations.findForBusiness(b.getId()).stream().map(ReservationResponse::from).toList();
    }

    /** Buyer cancels a reservation that hasn't been collected; stock goes back on sale. */
    @Transactional
    public ReservationResponse cancel(Long buyerId, Long id) {
        Reservation r = find(id);
        if (!r.getBuyer().getId().equals(buyerId)) throw new ApiException(HttpStatus.FORBIDDEN, "This is not your reservation");
        requireReserved(r);
        r.setStatus(ReservationStatus.CANCELLED);
        restoreStock(r, Instant.now());
        notifications.notify(r.getProduct().getBusiness().getOwner().getId(), NotificationType.ORDER_STATUS,
                r.getBuyer().getFullName() + " cancelled the reservation for " + r.getQuantity() + " x " + r.getProduct().getName() + ".",
                r.getProduct().getId());
        return ReservationResponse.from(r);
    }

    /** Seller confirms the buyer collected and paid. */
    @Transactional
    public ReservationResponse complete(Long ownerId, Long id) {
        Reservation r = find(id);
        if (!r.getProduct().getBusiness().getOwner().getId().equals(ownerId)) {
            throw new ApiException(HttpStatus.FORBIDDEN, "This reservation belongs to another business");
        }
        requireReserved(r);
        r.setStatus(ReservationStatus.COMPLETED);
        notifications.notify(r.getBuyer().getId(), NotificationType.ORDER_STATUS,
                "Pickup completed: " + r.getQuantity() + " x " + r.getProduct().getName() + ". Thank you for rescuing food!",
                r.getProduct().getId());
        return ReservationResponse.from(r);
    }

    /** Called by the scheduler: reservations not collected in time expire and stock returns. */
    public int expireOverdue(Instant now) {
        List<Long> ids = reservations.findByStatusAndPickupByLessThanEqual(ReservationStatus.RESERVED, now)
                .stream().map(Reservation::getId).toList();
        for (Long id : ids) {
            tx.executeWithoutResult(status -> {
                Reservation r = find(id);
                if (r.getStatus() != ReservationStatus.RESERVED) return;
                r.setStatus(ReservationStatus.EXPIRED);
                restoreStock(r, now);
                notifications.notify(r.getBuyer().getId(), NotificationType.ORDER_STATUS,
                        "Your reservation for " + r.getProduct().getName() + " expired because it was not collected in time.",
                        r.getProduct().getId());
            });
        }
        return ids.size();
    }

    private void restoreStock(Reservation r, Instant now) {
        Product p = products.findByIdForUpdate(r.getProduct().getId()).orElseThrow();
        boolean stillSelling = (p.getStatus() == ProductStatus.ACTIVE || p.getStatus() == ProductStatus.SOLD_OUT
                || p.getStatus() == ProductStatus.PAUSED) && p.getExpiresAt().isAfter(now);
        if (!stillSelling) return;
        p.setQuantityAvailable(Math.min(p.getQuantityTotal(), p.getQuantityAvailable() + r.getQuantity()));
        if (p.getStatus() == ProductStatus.SOLD_OUT) {
            p.setStatus(ProductStatus.ACTIVE);
            p.setLastDropAt(now);
        }
        products.saveAndFlush(p);
        publisher.publish(p, "STOCK", null);
    }

    private Reservation find(Long id) {
        return reservations.findById(id)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Reservation not found"));
    }

    private static void requireReserved(Reservation r) {
        if (r.getStatus() != ReservationStatus.RESERVED) {
            throw new ApiException(HttpStatus.CONFLICT, "This reservation is already " + r.getStatus().name().toLowerCase());
        }
    }
}
