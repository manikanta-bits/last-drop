package com.lastdrop.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * CORS = Cross-Origin Resource Sharing.
 *
 * Browsers block a web page on one address (http://localhost:5173, the React app)
 * from calling an API on another address (http://localhost:8080, this backend)
 * unless the API explicitly allows it. This class says which frontend addresses
 * are allowed. The list comes from the CORS_ALLOWED_ORIGINS environment variable,
 * so production can use the real Vercel/Netlify URL without changing code.
 *
 * Note: in Phase 4 Spring Security takes over request handling, and this
 * configuration moves into the security setup.
 */
@Configuration
public class CorsConfig implements WebMvcConfigurer {

    private final String[] allowedOrigins;

    public CorsConfig(@Value("${app.cors.allowed-origins}") String[] allowedOrigins) {
        this.allowedOrigins = allowedOrigins;
    }

    @Override
    public void addCorsMappings(CorsRegistry registry) {
        registry.addMapping("/api/**")
                .allowedOrigins(allowedOrigins)
                .allowedMethods("GET", "POST", "PUT", "DELETE", "OPTIONS")
                .allowedHeaders("*")
                .allowCredentials(true);
    }
}
