package com.lastdrop;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

import com.lastdrop.dto.Dtos.ReservationRequest;
import com.lastdrop.entity.Business;
import com.lastdrop.entity.Enums.BusinessStatus;
import com.lastdrop.entity.Enums.BusinessType;
import com.lastdrop.entity.Enums.Category;
import com.lastdrop.entity.Enums.NotificationType;
import com.lastdrop.entity.Enums.ProductStatus;
import com.lastdrop.entity.Product;
import com.lastdrop.entity.Role;
import com.lastdrop.entity.User;
import com.lastdrop.repository.*;
import com.lastdrop.security.JwtService;
import com.lastdrop.service.AuctionService;
import com.lastdrop.service.DemoModeService;
import com.lastdrop.service.ReservationService;

/**
 * End-to-end tests of the marketplace rules using the in-memory H2 database.
 * The auction clock is off in tests; we call the auction engine with chosen times instead.
 */
@SpringBootTest
@AutoConfigureMockMvc
class MarketplaceFlowTest {

    @Autowired MockMvc mvc;
    @Autowired JwtService jwt;
    @Autowired PasswordEncoder encoder;
    @Autowired UserRepository users;
    @Autowired BusinessRepository businesses;
    @Autowired ProductRepository products;
    @Autowired PriceHistoryRepository history;
    @Autowired ReservationRepository reservations;
    @Autowired NotificationRepository notifications;
    @Autowired AuctionService auctions;
    @Autowired ReservationService reservationService;
    @Autowired DemoModeService demoMode;

    User admin, buyer, ngo, seller;

    @BeforeEach
    void setUp() {
        notifications.deleteAll();
        reservations.deleteAll();
        history.deleteAll();
        products.deleteAll();
        businesses.deleteAll();
        users.deleteAll();
        admin = user("admin@t.com", Role.ADMIN);
        buyer = user("buyer@t.com", Role.BUYER);
        ngo = user("ngo@t.com", Role.NGO);
        seller = user("seller@t.com", Role.BUSINESS);
    }

    @AfterEach
    void resetDemoMode() {
        demoMode.setEnabled(false);
    }

    // ---------- 4. business approval + 3/15. role-based access ----------

    @Test
    void businessMustBeApprovedBeforeListingProducts() throws Exception {
        String shopJson = """
                {"name":"Test Bakery","businessType":"BAKERY","address":"1 Main Rd","phone":"9000000000",
                 "latitude":17.44,"longitude":78.39}""";
        mvc.perform(as(seller, post("/api/businesses")).content(shopJson))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status").value("PENDING"));
        assertThat(notifications.findTop50ByUserIdOrderByCreatedAtDescIdDesc(admin.getId())).isNotEmpty();

        mvc.perform(as(seller, post("/api/products")).content(productJson()))
                .andExpect(status().isForbidden());

        Long shopId = businesses.findByOwnerId(seller.getId()).orElseThrow().getId();
        mvc.perform(as(admin, put("/api/admin/businesses/" + shopId + "/approve")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("APPROVED"));

        mvc.perform(as(seller, post("/api/products")).content(productJson()))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.currentPrice").value(350.00))
                .andExpect(jsonPath("$.status").value("ACTIVE"));
    }

    @Test
    void usersCannotUseOtherRolesFeatures() throws Exception {
        mvc.perform(as(buyer, get("/api/admin/dashboard"))).andExpect(status().isForbidden());
        mvc.perform(as(buyer, post("/api/products")).content(productJson())).andExpect(status().isForbidden());
        mvc.perform(as(seller, post("/api/reservations")).content("{\"productId\":1,\"quantity\":1}")).andExpect(status().isForbidden());
        mvc.perform(get("/api/admin/users")).andExpect(status().isUnauthorized());
        mvc.perform(post("/api/reservations").contentType(MediaType.APPLICATION_JSON).content("{\"productId\":1,\"quantity\":1}"))
                .andExpect(status().isUnauthorized());
        mvc.perform(as(admin, get("/api/admin/dashboard"))).andExpect(status().isOk());
    }

    @Test
    void invalidPricingIsRejected() throws Exception {
        approvedShop(seller, 17.44, 78.39);
        String badJson = productJson().replace("\"minPrice\":100", "\"minPrice\":400");
        mvc.perform(as(seller, post("/api/products")).content(badJson))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Minimum price cannot be higher than the starting price"));
    }

    // ---------- 6/7. price reduction, no double drops, minimum price ----------

    @Test
    void priceDropsOncePerIntervalAndNeverBelowMinimum() {
        Instant now = Instant.now();
        Product p = product(approvedShop(seller, 17.44, 78.39), "350", "300", "25", 60, Duration.ofHours(10), 5);
        p.setLastDropAt(now.minus(Duration.ofMinutes(61)));
        products.save(p);

        assertThat(auctions.runPriceDrops(now)).isEqualTo(1);
        assertThat(price(p)).isEqualByComparingTo("325.00");

        assertThat(auctions.runPriceDrops(now)).isZero();                    // same interval again: no second drop
        assertThat(auctions.runPriceDrops(now.plusSeconds(30))).isZero();
        assertThat(price(p)).isEqualByComparingTo("325.00");

        auctions.runPriceDrops(now.plus(Duration.ofMinutes(60)));
        assertThat(price(p)).isEqualByComparingTo("300.00");                 // reached minimum
        auctions.runPriceDrops(now.plus(Duration.ofMinutes(120)));
        auctions.runPriceDrops(now.plus(Duration.ofMinutes(180)));
        assertThat(price(p)).isEqualByComparingTo("300.00");                 // never below minimum
        assertThat(history.findByProductIdOrderByChangedAtAsc(p.getId())).hasSize(2);
    }

    @Test
    void demoModeDropsEveryTenSeconds() {
        // whole milliseconds, so database rounding of tiny fractions can't shift the 10-second boundary
        Instant now = Instant.now().truncatedTo(java.time.temporal.ChronoUnit.MILLIS);
        Product p = product(approvedShop(seller, 17.44, 78.39), "350", "100", "25", 60, Duration.ofHours(10), 5);
        p.setLastDropAt(now);
        products.save(p);
        demoMode.setEnabled(true);

        assertThat(auctions.runPriceDrops(now.plusSeconds(5))).isZero();
        assertThat(auctions.runPriceDrops(now.plusSeconds(10))).isEqualTo(1);
        assertThat(auctions.runPriceDrops(now.plusSeconds(15))).isZero();
        assertThat(auctions.runPriceDrops(now.plusSeconds(20))).isEqualTo(1);
        assertThat(price(p)).isEqualByComparingTo("300.00");
    }

    @Test
    void pausedProductsDoNotDrop() {
        Instant now = Instant.now();
        Product p = product(approvedShop(seller, 17.44, 78.39), "350", "100", "25", 60, Duration.ofHours(10), 5);
        p.setLastDropAt(now.minus(Duration.ofHours(2)));
        p.setStatus(ProductStatus.PAUSED);
        products.save(p);
        assertThat(auctions.runPriceDrops(now)).isZero();
    }

    // ---------- 8. expiry ----------

    @Test
    void expiredAuctionsAreMarkedAndCannotBeReserved() throws Exception {
        Instant now = Instant.now();
        Product p = product(approvedShop(seller, 17.44, 78.39), "350", "100", "25", 60, Duration.ofMinutes(1), 5);

        assertThat(auctions.expireAuctions(now.plus(Duration.ofMinutes(2)))).isEqualTo(1);
        assertThat(products.findById(p.getId()).orElseThrow().getStatus()).isEqualTo(ProductStatus.EXPIRED);
        assertThat(notifications.findTop50ByUserIdOrderByCreatedAtDescIdDesc(seller.getId()))
                .anyMatch(n -> n.getType() == NotificationType.AUCTION_EXPIRED);

        mvc.perform(as(buyer, post("/api/reservations")).content(reserveJson(p.getId(), 1)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.message").value("Sorry, this deal has expired"));
    }

    // ---------- 11/13/14. reservations, cancellation, notifications ----------

    @Test
    void reservationLocksPriceReducesStockAndNotifiesSeller() throws Exception {
        Product p = product(approvedShop(seller, 17.44, 78.39), "350", "100", "25", 60, Duration.ofHours(5), 3);

        mvc.perform(as(buyer, post("/api/reservations")).content(reserveJson(p.getId(), 2)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.unitPrice").value(350.00))
                .andExpect(jsonPath("$.totalPrice").value(700.00))
                .andExpect(jsonPath("$.status").value("RESERVED"));
        assertThat(stock(p)).isEqualTo(1);
        assertThat(notifications.findTop50ByUserIdOrderByCreatedAtDescIdDesc(seller.getId()))
                .anyMatch(n -> n.getType() == NotificationType.RESERVATION);

        mvc.perform(as(ngo, post("/api/reservations")).content(reserveJson(p.getId(), 2)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.message").value("Only 1 left"));

        Long resId = reservations.findForBuyer(buyer.getId()).get(0).getId();
        mvc.perform(as(ngo, put("/api/reservations/" + resId + "/cancel"))).andExpect(status().isForbidden());
        mvc.perform(as(buyer, put("/api/reservations/" + resId + "/cancel")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("CANCELLED"));
        assertThat(stock(p)).isEqualTo(3);
        mvc.perform(as(buyer, put("/api/reservations/" + resId + "/cancel"))).andExpect(status().isConflict());

        mvc.perform(as(ngo, post("/api/reservations")).content(reserveJson(p.getId(), 3))).andExpect(status().isCreated());
        assertThat(products.findById(p.getId()).orElseThrow().getStatus()).isEqualTo(ProductStatus.SOLD_OUT);

        Long ngoRes = reservations.findForBuyer(ngo.getId()).get(0).getId();
        mvc.perform(as(seller, put("/api/reservations/" + ngoRes + "/complete")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("COMPLETED"));

        mvc.perform(as(buyer, get("/api/notifications"))).andExpect(status().isOk()).andExpect(jsonPath("$.length()").value(1));
        mvc.perform(as(ngo, get("/api/dashboard/buyer")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.impact.itemsRescued").value(3));
    }

    // ---------- 12. overselling ----------

    @Test
    void manyBuyersAtOnceCannotOversell() throws Exception {
        Product p = product(approvedShop(seller, 17.44, 78.39), "350", "100", "25", 60, Duration.ofHours(5), 3);
        List<User> crowd = new ArrayList<>();
        for (int i = 0; i < 8; i++) crowd.add(user("crowd" + i + "@t.com", Role.BUYER));

        ExecutorService pool = Executors.newFixedThreadPool(8);
        List<Future<Boolean>> results = new ArrayList<>();
        for (User u : crowd) {
            results.add(pool.submit(() -> {
                try {
                    reservationService.reserve(u.getId(), new ReservationRequest(p.getId(), 1));
                    return true;
                } catch (RuntimeException e) {
                    return false;
                }
            }));
        }
        int ok = 0;
        for (Future<Boolean> f : results) if (f.get()) ok++;
        pool.shutdown();

        assertThat(ok).isEqualTo(3);
        assertThat(stock(p)).isZero();
        assertThat(products.findById(p.getId()).orElseThrow().getStatus()).isEqualTo(ProductStatus.SOLD_OUT);
    }

    // ---------- 10. location-based discovery ----------

    @Test
    void nearbySearchOnlyReturnsShopsInsideRadius() throws Exception {
        Product near = product(approvedShop(seller, 17.4483, 78.3915), "100", "50", "5", 60, Duration.ofHours(5), 3);
        User farSeller = user("far@t.com", Role.BUSINESS);
        product(approvedShop(farSeller, 17.5800, 78.3915), "100", "50", "5", 60, Duration.ofHours(5), 3);   // ~15 km north

        mvc.perform(get("/api/products").param("lat", "17.4435").param("lng", "78.3772").param("radiusKm", "5"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].id").value(near.getId()))
                .andExpect(jsonPath("$[0].distanceKm").isNumber());

        mvc.perform(get("/api/products").param("lat", "17.4435").param("lng", "78.3772").param("radiusKm", "20"))
                .andExpect(jsonPath("$.length()").value(2));

        mvc.perform(get("/api/businesses/nearby").param("lat", "17.4435").param("lng", "78.3772").param("radiusKm", "5"))
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].liveDeals").value(1));
    }

    @Test
    void adminCanRemoveListingAndOpenReservationsAreCancelled() throws Exception {
        Product p = product(approvedShop(seller, 17.44, 78.39), "350", "100", "25", 60, Duration.ofHours(5), 3);
        mvc.perform(as(buyer, post("/api/reservations")).content(reserveJson(p.getId(), 1))).andExpect(status().isCreated());

        mvc.perform(as(admin, put("/api/admin/products/" + p.getId() + "/remove")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("REMOVED"));
        mvc.perform(get("/api/products/" + p.getId())).andExpect(status().isNotFound());
        mvc.perform(as(buyer, get("/api/reservations/my"))).andExpect(jsonPath("$[0].status").value("CANCELLED"));
    }

    // ---------- helpers ----------

    private MockHttpServletRequestBuilder as(User u, MockHttpServletRequestBuilder req) {
        return req.header("Authorization", "Bearer " + jwt.createToken(u)).contentType(MediaType.APPLICATION_JSON);
    }

    private User user(String email, Role role) {
        return users.save(new User(email, encoder.encode("Secret123"), email.split("@")[0], null, role));
    }

    private Business approvedShop(User owner, double lat, double lng) {
        Business b = new Business(owner);
        b.setName(owner.getFullName() + " shop");
        b.setBusinessType(BusinessType.BAKERY);
        b.setAddress("Test address");
        b.setLatitude(BigDecimal.valueOf(lat));
        b.setLongitude(BigDecimal.valueOf(lng));
        b.setStatus(BusinessStatus.APPROVED);
        return businesses.save(b);
    }

    private Product product(Business b, String start, String min, String drop, int intervalMin, Duration life, int qty) {
        Instant now = Instant.now();
        Product p = new Product(b);
        p.setName("Test cake");
        p.setCategory(Category.BAKERY);
        p.setQuantityTotal(qty);
        p.setQuantityAvailable(qty);
        p.setOriginalPrice(new BigDecimal("500.00"));
        p.setStartPrice(new BigDecimal(start));
        p.setMinPrice(new BigDecimal(min));
        p.setCurrentPrice(new BigDecimal(start));
        p.setDropAmount(new BigDecimal(drop));
        p.setDropIntervalMinutes(intervalMin);
        p.setAuctionStart(now);
        p.setLastDropAt(now);
        p.setExpiresAt(now.plus(life));
        return products.save(p);
    }

    private BigDecimal price(Product p) {
        return products.findById(p.getId()).orElseThrow().getCurrentPrice();
    }

    private int stock(Product p) {
        return products.findById(p.getId()).orElseThrow().getQuantityAvailable();
    }

    private static String productJson() {
        return """
               {"name":"Chocolate cake","category":"BAKERY","quantity":3,"originalPrice":500,"startPrice":350,
                "minPrice":100,"dropAmount":25,"dropIntervalMinutes":60,"expiresAt":"%s"}"""
                .formatted(Instant.now().plus(Duration.ofHours(6)));
    }

    private static String reserveJson(Long productId, int qty) {
        return "{\"productId\":" + productId + ",\"quantity\":" + qty + "}";
    }
}
