package com.lastdrop.util;

/** Distance between two map points (Haversine formula). */
public final class Geo {

    private static final double EARTH_RADIUS_KM = 6371.0;

    private Geo() {}

    public static double distanceKm(double lat1, double lng1, double lat2, double lng2) {
        double dLat = Math.toRadians(lat2 - lat1);
        double dLng = Math.toRadians(lng2 - lng1);
        double a = Math.pow(Math.sin(dLat / 2), 2)
                + Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2)) * Math.pow(Math.sin(dLng / 2), 2);
        double km = 2 * EARTH_RADIUS_KM * Math.asin(Math.sqrt(a));
        return Math.round(km * 100.0) / 100.0;
    }
}
