package com.lastdrop.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.lastdrop.entity.User;
import com.lastdrop.repository.UserRepository;

/**
 * Tests registration, login and token checks through real HTTP-style calls,
 * using the in-memory H2 database (no MySQL needed).
 */
@SpringBootTest
@AutoConfigureMockMvc
class AuthControllerTest {

    @Autowired private MockMvc mockMvc;
    @Autowired private UserRepository userRepository;
    @Autowired private ObjectMapper objectMapper;
    @Autowired private com.lastdrop.repository.NotificationRepository notifications;
    @Autowired private com.lastdrop.repository.ReservationRepository reservations;
    @Autowired private com.lastdrop.repository.PriceHistoryRepository history;
    @Autowired private com.lastdrop.repository.ProductRepository products;
    @Autowired private com.lastdrop.repository.BusinessRepository businesses;

    @BeforeEach
    void cleanDatabase() {
        // children first, so foreign keys are never broken
        notifications.deleteAll();
        reservations.deleteAll();
        history.deleteAll();
        products.deleteAll();
        businesses.deleteAll();
        userRepository.deleteAll();
    }

    // ---------- registration ----------

    @Test
    void registerCreatesUserWithHashedPasswordAndReturnsToken() throws Exception {
        register("Priya@Example.com", "Secret123", "BUYER")
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.token").isNotEmpty())
                .andExpect(jsonPath("$.user.email").value("priya@example.com"))   // stored in lower case
                .andExpect(jsonPath("$.user.role").value("BUYER"))
                .andExpect(jsonPath("$.user.passwordHash").doesNotExist());

        User saved = userRepository.findByEmail("priya@example.com").orElseThrow();
        assertThat(saved.getPasswordHash()).isNotEqualTo("Secret123").startsWith("$2a$");
    }

    @Test
    void duplicateEmailIsRejected() throws Exception {
        register("dup@example.com", "Secret123", "BUYER").andExpect(status().isCreated());

        register("DUP@example.com", "Other456", "NGO")
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.message").value("An account with this email already exists"));
    }

    @Test
    void invalidFieldsReturnFieldErrors() throws Exception {
        String body = """
                {"email":"not-an-email","password":"short","fullName":"","role":"BUYER"}
                """;
        mockMvc.perform(post("/api/auth/register").contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.fieldErrors.email").exists())
                .andExpect(jsonPath("$.fieldErrors.password").exists())
                .andExpect(jsonPath("$.fieldErrors.fullName").exists());
    }

    @Test
    void nobodyCanRegisterAsAdmin() throws Exception {
        register("sneaky@example.com", "Secret123", "ADMIN")
                .andExpect(status().isBadRequest());
        assertThat(userRepository.existsByEmail("sneaky@example.com")).isFalse();
    }

    // ---------- login ----------

    @Test
    void loginWithCorrectPasswordReturnsToken() throws Exception {
        register("ngo@example.com", "Secret123", "NGO");

        login("ngo@example.com", "Secret123")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token").isNotEmpty())
                .andExpect(jsonPath("$.user.role").value("NGO"));
    }

    @Test
    void loginWithWrongPasswordOrUnknownEmailFailsWithSameMessage() throws Exception {
        register("shop@example.com", "Secret123", "BUSINESS");

        login("shop@example.com", "WrongPass1")
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.message").value("Incorrect email or password"));
        login("nobody@example.com", "Secret123")
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.message").value("Incorrect email or password"));
    }

    @Test
    void disabledAccountCannotLogInAndOldTokenStopsWorking() throws Exception {
        String token = tokenOf(register("bad@example.com", "Secret123", "BUYER"));

        User user = userRepository.findByEmail("bad@example.com").orElseThrow();
        user.setEnabled(false);
        userRepository.save(user);

        login("bad@example.com", "Secret123").andExpect(status().isForbidden());
        mockMvc.perform(get("/api/auth/me").header("Authorization", "Bearer " + token))
                .andExpect(status().isUnauthorized());
    }

    // ---------- tokens ----------

    @Test
    void meRequiresAValidToken() throws Exception {
        String token = tokenOf(register("me@example.com", "Secret123", "BUYER"));

        mockMvc.perform(get("/api/auth/me"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.message").value("Please log in"));

        mockMvc.perform(get("/api/auth/me").header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.email").value("me@example.com"));
    }

    @Test
    void tamperedTokenIsRejected() throws Exception {
        String token = tokenOf(register("t@example.com", "Secret123", "BUYER"));
        String tampered = token.substring(0, token.length() - 2) + (token.endsWith("A") ? "BB" : "AA");

        mockMvc.perform(get("/api/auth/me").header("Authorization", "Bearer " + tampered))
                .andExpect(status().isUnauthorized());
    }

    // ---------- helpers ----------

    private ResultActions register(String email, String password, String role) throws Exception {
        String body = objectMapper.writeValueAsString(java.util.Map.of(
                "email", email, "password", password, "fullName", "Test User", "role", role));
        return mockMvc.perform(post("/api/auth/register").contentType(MediaType.APPLICATION_JSON).content(body));
    }

    private ResultActions login(String email, String password) throws Exception {
        String body = objectMapper.writeValueAsString(java.util.Map.of("email", email, "password", password));
        return mockMvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON).content(body));
    }

    private String tokenOf(ResultActions result) throws Exception {
        JsonNode json = objectMapper.readTree(result.andReturn().getResponse().getContentAsString());
        return json.get("token").asText();
    }
}
