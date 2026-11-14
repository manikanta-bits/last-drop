package com.lastdrop.websocket;

import java.math.BigDecimal;
import java.time.Instant;

import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import com.lastdrop.entity.Product;
import com.lastdrop.service.DemoModeService;
import com.lastdrop.util.Pricing;

/**
 * Broadcasts product changes over WebSocket.
 * Messages are sent only AFTER the database transaction commits, so browsers
 * never see a price that was rolled back.
 */
@Component
public class ProductEventPublisher {

    private final SimpMessagingTemplate messaging;
    private final DemoModeService demoMode;

    public ProductEventPublisher(SimpMessagingTemplate messaging, DemoModeService demoMode) {
        this.messaging = messaging;
        this.demoMode = demoMode;
    }

    public void publish(Product product, String type, BigDecimal oldPrice) {
        String businessName = product.getBusiness().getName();   // read now, while the database session is open
        Runnable send = () -> {
            ProductUpdateMessage msg = new ProductUpdateMessage(
                    product.getId(), type, product.getName(), businessName,
                    oldPrice, product.getCurrentPrice(), product.getOriginalPrice(), product.getMinPrice(),
                    Pricing.discountPercent(product.getOriginalPrice(), product.getCurrentPrice()),
                    product.getQuantityAvailable(), product.getStatus(), product.getLastDropAt(),
                    Pricing.nextDropAt(product, demoMode.isEnabled()), product.getExpiresAt(),
                    product.getVersion(), Instant.now());
            messaging.convertAndSend("/topic/products/" + product.getId(), msg);
            messaging.convertAndSend("/topic/products", msg);
        };

        if (TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override
                public void afterCommit() {
                    send.run();
                }
            });
        } else {
            send.run();
        }
    }
}
