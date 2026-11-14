package com.lastdrop.controller;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import com.lastdrop.dto.Dtos.ReservationRequest;
import com.lastdrop.dto.Dtos.ReservationResponse;
import com.lastdrop.security.AuthUser;
import com.lastdrop.service.ReservationService;

import jakarta.validation.Valid;

/**
 * BUYER/NGO: POST /api/reservations, GET /api/reservations/my, PUT /api/reservations/{id}/cancel
 * BUSINESS:  GET /api/reservations/business, PUT /api/reservations/{id}/complete
 */
@RestController
@RequestMapping("/api/reservations")
public class ReservationController {

    private final ReservationService service;

    public ReservationController(ReservationService service) {
        this.service = service;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasAnyRole('BUYER','NGO')")
    public ReservationResponse reserve(@AuthenticationPrincipal AuthUser me, @Valid @RequestBody ReservationRequest req) {
        return service.reserve(me.id(), req);
    }

    @GetMapping("/my")
    @PreAuthorize("hasAnyRole('BUYER','NGO')")
    public List<ReservationResponse> mine(@AuthenticationPrincipal AuthUser me) {
        return service.mine(me.id());
    }

    @PutMapping("/{id}/cancel")
    @PreAuthorize("hasAnyRole('BUYER','NGO')")
    public ReservationResponse cancel(@AuthenticationPrincipal AuthUser me, @PathVariable Long id) {
        return service.cancel(me.id(), id);
    }

    @GetMapping("/business")
    @PreAuthorize("hasRole('BUSINESS')")
    public List<ReservationResponse> forBusiness(@AuthenticationPrincipal AuthUser me) {
        return service.forMyBusiness(me.id());
    }

    @PutMapping("/{id}/complete")
    @PreAuthorize("hasRole('BUSINESS')")
    public ReservationResponse complete(@AuthenticationPrincipal AuthUser me, @PathVariable Long id) {
        return service.complete(me.id(), id);
    }
}
