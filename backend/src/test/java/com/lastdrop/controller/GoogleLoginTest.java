package com.lastdrop.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import com.lastdrop.entity.Role;
import com.lastdrop.exception.ApiException;
import com.lastdrop.repository.*;
import com.lastdrop.security.GoogleTokenVerifier;
import com.lastdrop.security.GoogleTokenVerifier.GoogleProfile;

/** "Continue with Google". Google itself is replaced by a mock so tests run offline. */
@SpringBootTest
@AutoConfigureMockMvc
class GoogleLoginTest {

    @Autowired MockMvc mvc;
    @Autowired UserRepository users;
    @Autowired NotificationRepository notifications;
    @Autowired ReservationRepository reservations;
    @Autowired PriceHistoryRepository history;
    @Autowired ProductRepository products;
    @Autowired BusinessRepository businesses;
    @MockitoBean GoogleTokenVerifier google;

    @BeforeEach
    void setUp() {
        notifications.deleteAll();
        reservations.deleteAll();
        history.deleteAll();
        products.deleteAll();
        businesses.deleteAll();
        users.deleteAll();
        when(google.isConfigured()).thenReturn(true);
        when(google.getClientId()).thenReturn("test-client-id");
        when(google.verify(eq("good-token"))).thenReturn(new GoogleProfile("Asha@Gmail.com", "Asha Rao"));
        when(google.verify(eq("bad-token"))).thenThrow(new ApiException(HttpStatus.UNAUTHORIZED, "Google sign-in failed. Please try again."));
    }

    @Test
    void firstGoogleSignInCreatesAccountWithChosenRole() throws Exception {
        google("good-token", "NGO")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token").isNotEmpty())
                .andExpect(jsonPath("$.user.email").value("asha@gmail.com"))
                .andExpect(jsonPath("$.user.fullName").value("Asha Rao"))
                .andExpect(jsonPath("$.user.role").value("NGO"));
        assertThat(users.findByEmail("asha@gmail.com")).isPresent();
    }

    @Test
    void secondGoogleSignInLogsIntoSameAccount() throws Exception {
        google("good-token", "BUSINESS").andExpect(status().isOk());
        google("good-token", "BUYER")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.user.role").value("BUSINESS"));   // existing role kept
        assertThat(users.count()).isEqualTo(1);
    }

    @Test
    void invalidTokenAndAdminRoleAreRejected() throws Exception {
        google("bad-token", "BUYER").andExpect(status().isUnauthorized());
        google("good-token", "ADMIN").andExpect(status().isBadRequest());
        assertThat(users.findByRole(Role.ADMIN)).isEmpty();
    }

    @Test
    void configTellsFrontendGoogleIsOn() throws Exception {
        mvc.perform(get("/api/auth/config"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.googleEnabled").value(true))
                .andExpect(jsonPath("$.googleClientId").value("test-client-id"));
    }

    private org.springframework.test.web.servlet.ResultActions google(String token, String role) throws Exception {
        return mvc.perform(post("/api/auth/google").contentType(MediaType.APPLICATION_JSON)
                .content("{\"credential\":\"" + token + "\",\"role\":\"" + role + "\"}"));
    }
}
