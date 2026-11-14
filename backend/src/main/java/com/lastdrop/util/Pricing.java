package com.lastdrop.util;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Duration;
import java.time.Instant;

import com.lastdrop.entity.Enums.ProductStatus;
import com.lastdrop.entity.Product;

/** Pure Dutch-auction maths. No database here, so it is easy to unit test. */
public final class Pricing {

    /** In demo mode every product drops every 10 seconds instead of its real interval. */
    public static final Duration DEMO_INTERVAL = Duration.ofSeconds(10);

    private Pricing() {}

    /** The next price: one drop lower, but never below the minimum. */
    public static BigDecimal nextPrice(BigDecimal current, BigDecimal drop, BigDecimal min) {
        return current.subtract(drop).max(min).setScale(2, RoundingMode.HALF_UP);
    }

    public static Duration interval(Product p, boolean demo) {
        return demo ? DEMO_INTERVAL : Duration.ofMinutes(p.getDropIntervalMinutes());
    }

    /** True if this product should drop its price now. */
    public static boolean isDropDue(Product p, Instant now, boolean demo) {
        return p.getStatus() == ProductStatus.ACTIVE
                && p.getQuantityAvailable() > 0
                && now.isBefore(p.getExpiresAt())
                && p.getCurrentPrice().compareTo(p.getMinPrice()) > 0
                && !now.isBefore(p.getLastDropAt().plus(interval(p, demo)));
    }

    /** When the next drop will happen, or null if there is no further drop. */
    public static Instant nextDropAt(Product p, boolean demo) {
        if (p.getStatus() != ProductStatus.ACTIVE || p.getCurrentPrice().compareTo(p.getMinPrice()) <= 0) {
            return null;
        }
        Instant next = p.getLastDropAt().plus(interval(p, demo));
        return next.isBefore(p.getExpiresAt()) ? next : null;
    }

    /** Whole-number discount compared with the normal shop price. */
    public static int discountPercent(BigDecimal original, BigDecimal current) {
        if (original.signum() <= 0) return 0;
        return original.subtract(current).multiply(BigDecimal.valueOf(100))
                .divide(original, 0, RoundingMode.HALF_UP).intValue();
    }
}
