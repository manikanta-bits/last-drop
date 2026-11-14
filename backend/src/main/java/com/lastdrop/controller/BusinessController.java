package com.lastdrop.controller;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import com.lastdrop.dto.Dtos.BusinessRequest;
import com.lastdrop.dto.Dtos.BusinessResponse;
import com.lastdrop.dto.Dtos.NearbyBusinessResponse;
import com.lastdrop.security.AuthUser;
import com.lastdrop.service.BusinessService;

import jakarta.validation.Valid;

/**
 * POST /api/businesses          create my shop (BUSINESS)
 * GET  /api/businesses/my       my shop (BUSINESS)
 * PUT  /api/businesses/my       edit my shop (BUSINESS)
 * GET  /api/businesses/nearby   shops with live deals near a point (public)
 */
@RestController
@RequestMapping("/api/businesses")
public class BusinessController {

    private final BusinessService service;

    public BusinessController(BusinessService service) {
        this.service = service;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasRole('BUSINESS')")
    public BusinessResponse create(@AuthenticationPrincipal AuthUser me, @Valid @RequestBody BusinessRequest req) {
        return service.create(me.id(), req);
    }

    @GetMapping("/my")
    @PreAuthorize("hasRole('BUSINESS')")
    public BusinessResponse mine(@AuthenticationPrincipal AuthUser me) {
        return service.getMine(me.id());
    }

    @PutMapping("/my")
    @PreAuthorize("hasRole('BUSINESS')")
    public BusinessResponse update(@AuthenticationPrincipal AuthUser me, @Valid @RequestBody BusinessRequest req) {
        return service.updateMine(me.id(), req);
    }

    @GetMapping("/nearby")
    public List<NearbyBusinessResponse> nearby(@RequestParam double lat, @RequestParam double lng,
                                               @RequestParam(defaultValue = "5") double radiusKm) {
        return service.nearby(lat, lng, radiusKm);
    }
}
