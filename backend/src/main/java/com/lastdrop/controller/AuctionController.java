package com.lastdrop.controller;

import java.util.List;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.lastdrop.dto.Dtos.PriceHistoryResponse;
import com.lastdrop.dto.Dtos.ProductResponse;
import com.lastdrop.service.ProductService;

/** Auction views (each product is its own auction, so the id is the product id). Public. */
@RestController
@RequestMapping("/api/auctions")
public class AuctionController {

    private final ProductService service;

    public AuctionController(ProductService service) {
        this.service = service;
    }

    @GetMapping("/active")
    public List<ProductResponse> active() {
        return service.listLive(null, null, null, null, null, null, null);
    }

    @GetMapping("/{id}")
    public ProductResponse get(@PathVariable Long id) {
        return service.get(id, null, null);
    }

    @GetMapping("/{id}/history")
    public List<PriceHistoryResponse> history(@PathVariable Long id) {
        return service.history(id);
    }
}
