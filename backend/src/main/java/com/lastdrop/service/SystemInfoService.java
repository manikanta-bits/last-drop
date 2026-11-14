package com.lastdrop.service;

import java.lang.management.ManagementFactory;
import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.time.Duration;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.sql.DataSource;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

/**
 * Admin "System info": which database the backend is really connected to, so it can be
 * compared with the database shown in Railway / MySQL Workbench. Never includes the password.
 */
@Service
public class SystemInfoService {

    private static final Pattern HOST_PORT_DB = Pattern.compile("jdbc:[a-z0-9]+://([^:/?]+)(?::(\\d+))?/([^?;]*)");

    private final DataSource dataSource;
    private final JdbcTemplate jdbc;
    private final DemoModeService demoMode;

    public SystemInfoService(DataSource dataSource, JdbcTemplate jdbc, DemoModeService demoMode) {
        this.dataSource = dataSource;
        this.jdbc = jdbc;
        this.demoMode = demoMode;
    }

    public Map<String, Object> info() {
        Map<String, Object> db = new LinkedHashMap<>();
        try (Connection c = dataSource.getConnection()) {
            DatabaseMetaData meta = c.getMetaData();
            String url = meta.getURL();
            Matcher m = HOST_PORT_DB.matcher(url == null ? "" : url);
            if (m.find()) {
                db.put("host", m.group(1));
                db.put("port", m.group(2) == null ? "default" : m.group(2));
                db.put("configuredDatabase", m.group(3));
            }
            db.put("product", meta.getDatabaseProductName() + " " + meta.getDatabaseProductVersion());
            db.put("user", meta.getUserName());
            db.put("connected", true);
        } catch (Exception e) {
            db.put("connected", false);
            db.put("error", e.getClass().getSimpleName());
        }
        db.put("currentDatabase", query("SELECT DATABASE()"));
        db.put("serverHostname", query("SELECT @@hostname"));   // MySQL's own container name

        Map<String, Object> rows = new LinkedHashMap<>();
        for (String table : new String[] {"users", "businesses", "products", "reservations", "notifications", "price_history"}) {
            rows.put(table, query("SELECT COUNT(*) FROM " + table));
        }
        rows.put("latestSignup", query("SELECT MAX(created_at) FROM users"));

        Map<String, Object> app = new LinkedHashMap<>();
        app.put("serverTimeUtc", Instant.now().toString());
        app.put("uptime", formatUptime(Duration.ofMillis(ManagementFactory.getRuntimeMXBean().getUptime())));
        app.put("java", System.getProperty("java.version"));
        app.put("demoMode", demoMode.isEnabled());

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("database", db);
        result.put("rows", rows);
        result.put("application", app);
        return result;
    }

    /** Runs a one-value query; returns "n/a" instead of failing (e.g. on test databases). */
    private Object query(String sql) {
        try {
            Object value = jdbc.queryForObject(sql, Object.class);
            return value == null ? "-" : value.toString();
        } catch (Exception e) {
            return "n/a";
        }
    }

    private static String formatUptime(Duration d) {
        return d.toHours() + "h " + d.toMinutesPart() + "m";
    }
}
