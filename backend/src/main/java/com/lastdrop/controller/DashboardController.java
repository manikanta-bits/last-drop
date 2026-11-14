package com.lastdrop.controller;

import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.lastdrop.dto.Dtos.BusinessDashboard;
import com.lastdrop.dto.Dtos.BuyerDashboard;
import com.lastdrop.security.AuthUser;
import com.lastdrop.service.DashboardService;

@RestController
@RequestMapping("/api/dashboard")
public class DashboardController {

    private final DashboardService service;

    public DashboardController(DashboardService service) {
        this.service = service;
    }

    @GetMapping("/buyer")
    @PreAuthorize("hasAnyRole('BUYER','NGO')")
    public BuyerDashboard buyer(@AuthenticationPrincipal AuthUser me) {
        return service.buyer(me.id());
    }

    @GetMapping("/business")
    @PreAuthorize("hasRole('BUSINESS')")
    public BusinessDashboard business(@AuthenticationPrincipal AuthUser me) {
        return service.business(me.id());
    }
}
