package com.lastdrop.controller;

import java.util.List;

import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import com.lastdrop.dto.Dtos.AdminDashboard;
import com.lastdrop.dto.Dtos.AdminUserResponse;
import com.lastdrop.dto.Dtos.BusinessResponse;
import com.lastdrop.dto.Dtos.DemoModeResponse;
import com.lastdrop.dto.Dtos.EnabledRequest;
import com.lastdrop.dto.Dtos.ProductResponse;
import com.lastdrop.entity.Enums.BusinessStatus;
import com.lastdrop.security.AuthUser;
import com.lastdrop.service.BusinessService;
import com.lastdrop.service.DashboardService;
import com.lastdrop.service.DemoModeService;
import com.lastdrop.service.ProductService;
import com.lastdrop.service.UserAdminService;
import com.lastdrop.util.Pricing;

import jakarta.validation.Valid;

/** Everything here is for ADMIN accounts only. */
@RestController
@RequestMapping("/api/admin")
@PreAuthorize("hasRole('ADMIN')")
public class AdminController {

    private final DashboardService dashboard;
    private final UserAdminService users;
    private final BusinessService businesses;
    private final ProductService products;
    private final DemoModeService demoMode;

    public AdminController(DashboardService dashboard, UserAdminService users, BusinessService businesses,
                           ProductService products, DemoModeService demoMode) {
        this.dashboard = dashboard;
        this.users = users;
        this.businesses = businesses;
        this.products = products;
        this.demoMode = demoMode;
    }

    @GetMapping("/dashboard")
    public AdminDashboard dashboard() {
        return dashboard.admin();
    }

    @GetMapping("/users")
    public List<AdminUserResponse> users() {
        return users.list();
    }

    @PutMapping("/users/{id}/enabled")
    public AdminUserResponse setEnabled(@AuthenticationPrincipal AuthUser me, @PathVariable Long id,
                                        @Valid @RequestBody EnabledRequest req) {
        return users.setEnabled(me.id(), id, req.enabled());
    }

    @GetMapping("/businesses")
    public List<BusinessResponse> businesses(@RequestParam(required = false) BusinessStatus status) {
        return businesses.adminList(status);
    }

    @PutMapping("/businesses/{id}/approve")
    public BusinessResponse approve(@PathVariable Long id) {
        return businesses.setStatus(id, BusinessStatus.APPROVED);
    }

    @PutMapping("/businesses/{id}/reject")
    public BusinessResponse reject(@PathVariable Long id) {
        return businesses.setStatus(id, BusinessStatus.REJECTED);
    }

    @GetMapping("/products")
    public List<ProductResponse> products() {
        return products.adminList();
    }

    @PutMapping("/products/{id}/remove")
    public ProductResponse remove(@PathVariable Long id) {
        return products.adminRemove(id);
    }

    @GetMapping("/demo-mode")
    public DemoModeResponse getDemoMode() {
        return new DemoModeResponse(demoMode.isEnabled(), Pricing.DEMO_INTERVAL.toSeconds());
    }

    @PutMapping("/demo-mode")
    public DemoModeResponse setDemoMode(@Valid @RequestBody EnabledRequest req) {
        demoMode.setEnabled(req.enabled());
        return getDemoMode();
    }
}
