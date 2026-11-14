package com.lastdrop.controller;

import java.math.BigDecimal;
import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import com.lastdrop.dto.Dtos.PriceHistoryResponse;
import com.lastdrop.dto.Dtos.ProductRequest;
import com.lastdrop.dto.Dtos.ProductResponse;
import com.lastdrop.dto.Dtos.ProductUpdateRequest;
import com.lastdrop.entity.Enums.Category;
import com.lastdrop.security.AuthUser;
import com.lastdrop.service.ProductService;

import jakarta.validation.Valid;

/**
 * Public:   GET /api/products (live deals, filters), GET /api/products/{id}, GET /api/products/{id}/history
 * BUSINESS: GET /api/products/mine, POST, PUT /{id}, DELETE /{id} (cancel), PUT /{id}/pause, PUT /{id}/resume
 */
@RestController
@RequestMapping("/api/products")
public class ProductController {

    private final ProductService service;

    public ProductController(ProductService service) {
        this.service = service;
    }

    @GetMapping
    public List<ProductResponse> list(@RequestParam(required = false) String q,
                                      @RequestParam(required = false) Category category,
                                      @RequestParam(required = false) Double lat,
                                      @RequestParam(required = false) Double lng,
                                      @RequestParam(required = false) Double radiusKm,
                                      @RequestParam(required = false) BigDecimal maxPrice,
                                      @RequestParam(required = false) String sort) {
        return service.listLive(q, category, lat, lng, radiusKm, maxPrice, sort);
    }

    @GetMapping("/mine")
    @PreAuthorize("hasRole('BUSINESS')")
    public List<ProductResponse> mine(@AuthenticationPrincipal AuthUser me) {
        return service.mine(me.id());
    }

    @GetMapping("/{id}")
    public ProductResponse get(@PathVariable Long id,
                               @RequestParam(required = false) Double lat,
                               @RequestParam(required = false) Double lng) {
        return service.get(id, lat, lng);
    }

    @GetMapping("/{id}/history")
    public List<PriceHistoryResponse> history(@PathVariable Long id) {
        return service.history(id);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasRole('BUSINESS')")
    public ProductResponse create(@AuthenticationPrincipal AuthUser me, @Valid @RequestBody ProductRequest req) {
        return service.create(me.id(), req);
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('BUSINESS')")
    public ProductResponse update(@AuthenticationPrincipal AuthUser me, @PathVariable Long id,
                                  @Valid @RequestBody ProductUpdateRequest req) {
        return service.update(me.id(), id, req);
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('BUSINESS')")
    public ProductResponse cancel(@AuthenticationPrincipal AuthUser me, @PathVariable Long id) {
        return service.cancel(me.id(), id);
    }

    @PutMapping("/{id}/pause")
    @PreAuthorize("hasRole('BUSINESS')")
    public ProductResponse pause(@AuthenticationPrincipal AuthUser me, @PathVariable Long id) {
        return service.pause(me.id(), id);
    }

    @PutMapping("/{id}/resume")
    @PreAuthorize("hasRole('BUSINESS')")
    public ProductResponse resume(@AuthenticationPrincipal AuthUser me, @PathVariable Long id) {
        return service.resume(me.id(), id);
    }
}
