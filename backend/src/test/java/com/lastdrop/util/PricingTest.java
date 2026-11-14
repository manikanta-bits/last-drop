package com.lastdrop.util;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;

import org.junit.jupiter.api.Test;

/** Unit tests for the pure auction maths (no Spring, no database). */
class PricingTest {

    private static BigDecimal rs(String v) {
        return new BigDecimal(v);
    }

    @Test
    void dropsByTheDropAmount() {
        assertThat(Pricing.nextPrice(rs("350.00"), rs("25.00"), rs("100.00"))).isEqualByComparingTo("325.00");
    }

    @Test
    void neverGoesBelowMinimum() {
        assertThat(Pricing.nextPrice(rs("110.00"), rs("25.00"), rs("100.00"))).isEqualByComparingTo("100.00");
        assertThat(Pricing.nextPrice(rs("100.00"), rs("25.00"), rs("100.00"))).isEqualByComparingTo("100.00");
    }

    @Test
    void discountIsRoundedPercentOfOriginal() {
        assertThat(Pricing.discountPercent(rs("500.00"), rs("350.00"))).isEqualTo(30);
        assertThat(Pricing.discountPercent(rs("60.00"), rs("45.00"))).isEqualTo(25);
    }

    @Test
    void haversineDistanceIsReasonable() {
        // Madhapur to Banjara Hills in Hyderabad is roughly 6 km
        double km = Geo.distanceKm(17.4483, 78.3915, 17.4156, 78.4347);
        assertThat(km).isBetween(5.0, 7.0);
    }
}
