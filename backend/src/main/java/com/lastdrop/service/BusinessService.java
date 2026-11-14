package com.lastdrop.service;

import java.time.Instant;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.lastdrop.dto.Dtos.BusinessRequest;
import com.lastdrop.dto.Dtos.BusinessResponse;
import com.lastdrop.dto.Dtos.NearbyBusinessResponse;
import com.lastdrop.entity.Business;
import com.lastdrop.entity.Enums.BusinessStatus;
import com.lastdrop.entity.Enums.NotificationType;
import com.lastdrop.entity.Enums.ProductStatus;
import com.lastdrop.entity.Product;
import com.lastdrop.exception.ApiException;
import com.lastdrop.repository.BusinessRepository;
import com.lastdrop.repository.ProductRepository;
import com.lastdrop.repository.UserRepository;
import com.lastdrop.util.Geo;

@Service
public class BusinessService {

    private final BusinessRepository businesses;
    private final UserRepository users;
    private final ProductRepository products;
    private final NotificationService notifications;

    public BusinessService(BusinessRepository businesses, UserRepository users, ProductRepository products,
                           NotificationService notifications) {
        this.businesses = businesses;
        this.users = users;
        this.products = products;
        this.notifications = notifications;
    }

    /** A BUSINESS user creates their shop profile. It starts as PENDING until an admin approves it. */
    @Transactional
    public BusinessResponse create(Long ownerId, BusinessRequest req) {
        if (businesses.existsByOwnerId(ownerId)) {
            throw new ApiException(HttpStatus.CONFLICT, "You already have a business profile");
        }
        Business b = new Business(users.getReferenceById(ownerId));
        apply(b, req);
        businesses.save(b);
        notifications.notify(ownerId, NotificationType.ACCOUNT,
                "Your business " + b.getName() + " is waiting for admin approval.", null);
        notifications.notifyAdmins(NotificationType.ACCOUNT,
                "New business registration: " + b.getName() + " is waiting for approval.");
        return BusinessResponse.from(b);
    }

    @Transactional(readOnly = true)
    public BusinessResponse getMine(Long ownerId) {
        return BusinessResponse.from(requireMine(ownerId));
    }

    /** Editing a rejected business sends it back to the admin for review. */
    @Transactional
    public BusinessResponse updateMine(Long ownerId, BusinessRequest req) {
        Business b = requireMine(ownerId);
        apply(b, req);
        if (b.getStatus() == BusinessStatus.REJECTED) {
            b.setStatus(BusinessStatus.PENDING);
            notifications.notifyAdmins(NotificationType.ACCOUNT, b.getName() + " updated its details and is waiting for approval again.");
        }
        return BusinessResponse.from(b);
    }

    public Business requireMine(Long ownerId) {
        return businesses.findByOwnerId(ownerId)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Create your business profile first"));
    }

    /** Approved shops within the radius that have at least one live deal, nearest first. */
    @Transactional(readOnly = true)
    public List<NearbyBusinessResponse> nearby(double lat, double lng, double radiusKm) {
        List<Product> live = products.findLive(Instant.now(), ProductStatus.ACTIVE, BusinessStatus.APPROVED);
        Map<Business, Long> dealsPerShop = live.stream()
                .collect(Collectors.groupingBy(Product::getBusiness, Collectors.counting()));
        return dealsPerShop.entrySet().stream()
                .map(e -> {
                    Business b = e.getKey();
                    double km = Geo.distanceKm(lat, lng, b.getLatitude().doubleValue(), b.getLongitude().doubleValue());
                    return new NearbyBusinessResponse(b.getId(), b.getName(), b.getBusinessType(), b.getAddress(),
                            b.getLatitude(), b.getLongitude(), km, e.getValue());
                })
                .filter(n -> n.distanceKm() <= radiusKm)
                .sorted(Comparator.comparingDouble(NearbyBusinessResponse::distanceKm))
                .toList();
    }

    // ---------------- admin ----------------

    @Transactional(readOnly = true)
    public List<BusinessResponse> adminList(BusinessStatus status) {
        List<Business> list = status == null ? businesses.findAllWithOwner() : businesses.findByStatusWithOwner(status);
        return list.stream().map(BusinessResponse::from).toList();
    }

    @Transactional
    public BusinessResponse setStatus(Long id, BusinessStatus status) {
        Business b = businesses.findById(id)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Business not found"));
        b.setStatus(status);
        String msg = status == BusinessStatus.APPROVED
                ? "Your business " + b.getName() + " has been approved. You can now list products."
                : "Your business " + b.getName() + " was not approved. Please check your details and update them.";
        notifications.notify(b.getOwner().getId(), NotificationType.ACCOUNT, msg, null);
        return BusinessResponse.from(b);
    }

    private static void apply(Business b, BusinessRequest req) {
        b.setName(req.name().trim());
        b.setBusinessType(req.businessType());
        b.setAddress(req.address().trim());
        b.setPhone(req.phone() == null || req.phone().isBlank() ? null : req.phone().trim());
        b.setLatitude(req.latitude());
        b.setLongitude(req.longitude());
    }
}
