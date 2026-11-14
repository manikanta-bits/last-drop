package com.lastdrop.security;

import java.util.Map;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

import com.lastdrop.exception.ApiException;

/**
 * Checks a "Sign in with Google" ID token with Google's tokeninfo service.
 * Google rejects fake or expired tokens; we also check the token was made for OUR app
 * (audience = our client id) and that Google has verified the email address.
 */
@Component
public class GoogleTokenVerifier {

    public record GoogleProfile(String email, String name) {
    }

    private final String clientId;
    private final RestClient http = RestClient.create("https://oauth2.googleapis.com");

    public GoogleTokenVerifier(@Value("${app.google.client-id:}") String clientId) {
        this.clientId = clientId == null ? "" : clientId.trim();
    }

    public boolean isConfigured() {
        return !clientId.isEmpty();
    }

    public String getClientId() {
        return clientId;
    }

    public GoogleProfile verify(String idToken) {
        if (!isConfigured()) {
            throw new ApiException(HttpStatus.SERVICE_UNAVAILABLE, "Google sign-in is not set up on this server");
        }
        Map<String, Object> info;
        try {
            info = http.get()
                    .uri(uri -> uri.path("/tokeninfo").queryParam("id_token", idToken).build())
                    .retrieve()
                    .body(new ParameterizedTypeReference<Map<String, Object>>() {});
        } catch (RestClientException e) {
            throw new ApiException(HttpStatus.UNAUTHORIZED, "Google sign-in failed. Please try again.");
        }
        if (info == null
                || !clientId.equals(String.valueOf(info.get("aud")))
                || !String.valueOf(info.get("iss")).endsWith("accounts.google.com")
                || !"true".equals(String.valueOf(info.get("email_verified")))) {
            throw new ApiException(HttpStatus.UNAUTHORIZED, "Google sign-in failed. Please try again.");
        }
        String email = String.valueOf(info.get("email"));
        Object name = info.get("name");
        return new GoogleProfile(email, name == null ? email.split("@")[0] : String.valueOf(name));
    }
}
