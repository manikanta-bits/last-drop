package com.lastdrop.scheduler;

import java.time.Instant;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import com.lastdrop.service.AuctionService;
import com.lastdrop.service.ReservationService;

/**
 * The auction "clock". Every 2 seconds it asks the services to:
 *   1. drop prices that are due,  2. expire finished auctions,  3. expire uncollected reservations.
 * Checking often is cheap: a product only drops when its own interval has passed.
 * Switched off in tests (app.scheduler.enabled=false) so tests control time themselves.
 */
@Component
@ConditionalOnProperty(name = "app.scheduler.enabled", havingValue = "true", matchIfMissing = true)
public class AuctionScheduler {

    private static final Logger log = LoggerFactory.getLogger(AuctionScheduler.class);

    private final AuctionService auctions;
    private final ReservationService reservations;

    public AuctionScheduler(AuctionService auctions, ReservationService reservations) {
        this.auctions = auctions;
        this.reservations = reservations;
    }

    @Scheduled(fixedDelayString = "${app.scheduler.tick-ms:2000}")
    public void tick() {
        Instant now = Instant.now();
        try {
            int dropped = auctions.runPriceDrops(now);
            int expired = auctions.expireAuctions(now);
            int lapsed = reservations.expireOverdue(now);
            if (dropped + expired + lapsed > 0) {
                log.info("Auction tick: {} price drops, {} auctions expired, {} reservations expired", dropped, expired, lapsed);
            }
        } catch (RuntimeException e) {
            log.error("Auction tick failed; will try again next tick", e);
        }
    }
}
