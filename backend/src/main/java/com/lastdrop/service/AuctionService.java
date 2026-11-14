package com.lastdrop.service;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionTemplate;

import com.lastdrop.entity.Enums.NotificationType;
import com.lastdrop.entity.Enums.ProductStatus;
import com.lastdrop.entity.PriceHistory;
import com.lastdrop.entity.Product;
import com.lastdrop.repository.PriceHistoryRepository;
import com.lastdrop.repository.ProductRepository;
import com.lastdrop.util.Pricing;
import com.lastdrop.websocket.ProductEventPublisher;

/**
 * THE DUTCH AUCTION ENGINE. All price changes happen here, never in the browser.
 *
 * Safety rules:
 *  - only ACTIVE, in-stock, unexpired products drop
 *  - never below the minimum price (Pricing.nextPrice)
 *  - at most ONE drop per product per run, and only when lastDropAt + interval has passed.
 *    Because lastDropAt moves forward with each drop, running twice for the same
 *    interval cannot drop twice.
 *  - each product is updated in its own transaction with optimistic locking, so a
 *    reservation happening at the same moment wins and the drop is retried next run.
 */
@Service
public class AuctionService {

    private static final Logger log = LoggerFactory.getLogger(AuctionService.class);

    private final ProductRepository products;
    private final PriceHistoryRepository history;
    private final ProductEventPublisher publisher;
    private final NotificationService notifications;
    private final DemoModeService demoMode;
    private final TransactionTemplate tx;

    public AuctionService(ProductRepository products, PriceHistoryRepository history, ProductEventPublisher publisher,
                          NotificationService notifications, DemoModeService demoMode, TransactionTemplate tx) {
        this.products = products;
        this.history = history;
        this.publisher = publisher;
        this.notifications = notifications;
        this.demoMode = demoMode;
        this.tx = tx;
    }

    /** Lowers the price of every auction whose drop is due. Returns how many prices dropped. */
    public int runPriceDrops(Instant now) {
        boolean demo = demoMode.isEnabled();
        int dropped = 0;
        for (Product candidate : products.findByStatus(ProductStatus.ACTIVE)) {
            if (!Pricing.isDropDue(candidate, now, demo)) continue;
            try {
                if (Boolean.TRUE.equals(tx.execute(status -> dropIfDue(candidate.getId(), now, demo)))) dropped++;
            } catch (OptimisticLockingFailureException e) {
                log.debug("Product {} changed at the same time; will retry next run", candidate.getId());
            }
        }
        return dropped;
    }

    /** Must run inside a transaction. Re-reads the product so the check uses fresh data. */
    private boolean dropIfDue(Long productId, Instant now, boolean demo) {
        Product p = products.findWithBusiness(productId).orElse(null);
        if (p == null || !Pricing.isDropDue(p, now, demo)) return false;

        BigDecimal oldPrice = p.getCurrentPrice();
        BigDecimal newPrice = Pricing.nextPrice(oldPrice, p.getDropAmount(), p.getMinPrice());
        p.setCurrentPrice(newPrice);
        // Real mode keeps the hourly rhythm (start + 1h, + 2h ...). Demo mode just counts from now.
        p.setLastDropAt(demo ? now : p.getLastDropAt().plus(Pricing.interval(p, false)));
        history.save(new PriceHistory(p, oldPrice, newPrice, now));
        products.saveAndFlush(p);
        publisher.publish(p, "PRICE_DROP", oldPrice);
        return true;
    }

    /** Marks auctions past their deadline as EXPIRED and tells the seller. */
    public int expireAuctions(Instant now) {
        Integer count = tx.execute(status -> {
            List<Product> due = products.findByStatusInAndExpiresAtLessThanEqual(
                    List.of(ProductStatus.ACTIVE, ProductStatus.PAUSED), now);
            for (Product p : due) {
                p.setStatus(ProductStatus.EXPIRED);
                if (p.getQuantityAvailable() > 0) {
                    notifications.notify(p.getBusiness().getOwner().getId(), NotificationType.AUCTION_EXPIRED,
                            "Your auction for " + p.getName() + " expired with " + p.getQuantityAvailable() + " left unsold.", p.getId());
                }
                publisher.publish(p, "STATUS", null);
            }
            return due.size();
        });
        return count == null ? 0 : count;
    }
}
