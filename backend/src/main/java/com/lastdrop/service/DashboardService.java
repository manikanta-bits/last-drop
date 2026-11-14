package com.lastdrop.service;

import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.lastdrop.dto.Dtos.AdminDashboard;
import com.lastdrop.dto.Dtos.BusinessDashboard;
import com.lastdrop.dto.Dtos.BuyerDashboard;
import com.lastdrop.dto.Dtos.Impact;
import com.lastdrop.entity.Business;
import com.lastdrop.entity.Enums.BusinessStatus;
import com.lastdrop.entity.Enums.ProductStatus;
import com.lastdrop.entity.Enums.ReservationStatus;
import com.lastdrop.entity.Role;
import com.lastdrop.repository.BusinessRepository;
import com.lastdrop.repository.ProductRepository;
import com.lastdrop.repository.ReservationRepository;
import com.lastdrop.repository.UserRepository;

/**
 * Numbers for the three dashboards.
 * Environmental figures are ESTIMATES based on the documented assumptions below,
 * not measured results.
 */
@Service
public class DashboardService {

    static final double KG_PER_ITEM = 0.5;   // assumed average weight of one rescued item
    static final double CO2_PER_KG = 2.5;    // assumed kg CO2-equivalent avoided per kg of food not wasted
    static final String ASSUMPTIONS = "Estimate: each collected item counted as 0.5 kg of food; "
            + "2.5 kg CO2e avoided per kg of food not wasted. Not measured values.";

    private static final List<ReservationStatus> NOT_CANCELLED = List.of(ReservationStatus.RESERVED, ReservationStatus.COMPLETED);

    private final UserRepository users;
    private final BusinessRepository businesses;
    private final ProductRepository products;
    private final ReservationRepository reservations;
    private final BusinessService businessService;
    private final DemoModeService demoMode;

    public DashboardService(UserRepository users, BusinessRepository businesses, ProductRepository products,
                            ReservationRepository reservations, BusinessService businessService, DemoModeService demoMode) {
        this.users = users;
        this.businesses = businesses;
        this.products = products;
        this.reservations = reservations;
        this.businessService = businessService;
        this.demoMode = demoMode;
    }

    @Transactional(readOnly = true)
    public AdminDashboard admin() {
        long rescued = reservations.sumQuantity(ReservationStatus.COMPLETED);
        return new AdminDashboard(
                users.count(),
                users.findByRole(Role.BUYER).size(),
                users.findByRole(Role.NGO).size(),
                users.findByRole(Role.BUSINESS).size(),
                businesses.countByStatus(BusinessStatus.APPROVED),
                businesses.countByStatus(BusinessStatus.PENDING),
                products.count(),
                products.countByStatus(ProductStatus.ACTIVE),
                products.countByStatus(ProductStatus.SOLD_OUT),
                products.countByStatus(ProductStatus.EXPIRED),
                reservations.countByStatus(ReservationStatus.RESERVED),
                reservations.countByStatus(ReservationStatus.COMPLETED),
                reservations.sumSavings(NOT_CANCELLED),
                impact(rescued),
                demoMode.isEnabled());
    }

    @Transactional(readOnly = true)
    public BusinessDashboard business(Long ownerId) {
        Business b = businessService.requireMine(ownerId);
        return new BusinessDashboard(
                b.getStatus(),
                products.countByBusinessIdAndStatus(b.getId(), ProductStatus.ACTIVE),
                products.sumQuantityByBusiness(b.getId()),
                reservations.sumQuantityForBusiness(b.getId(), ReservationStatus.COMPLETED),
                reservations.sumRevenueForBusiness(b.getId(), ReservationStatus.COMPLETED),
                reservations.countByProductBusinessIdAndStatus(b.getId(), ReservationStatus.RESERVED));
    }

    @Transactional(readOnly = true)
    public BuyerDashboard buyer(Long buyerId) {
        long rescued = reservations.sumQuantityForBuyer(buyerId, List.of(ReservationStatus.COMPLETED));
        return new BuyerDashboard(
                reservations.countByBuyerIdAndStatus(buyerId, ReservationStatus.RESERVED),
                reservations.countByBuyerIdAndStatus(buyerId, ReservationStatus.COMPLETED),
                reservations.sumSavingsForBuyer(buyerId, NOT_CANCELLED),
                impact(rescued));
    }

    static Impact impact(long itemsRescued) {
        double kg = Math.round(itemsRescued * KG_PER_ITEM * 10) / 10.0;
        double co2 = Math.round(kg * CO2_PER_KG * 10) / 10.0;
        return new Impact(itemsRescued, kg, co2, ASSUMPTIONS);
    }
}
