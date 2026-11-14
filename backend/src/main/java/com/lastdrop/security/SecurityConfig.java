package com.lastdrop.security;

import java.io.IOException;
import java.time.Instant;
import java.util.List;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import com.lastdrop.repository.UserRepository;

import jakarta.servlet.http.HttpServletResponse;

/**
 * The security rules for the whole backend.
 *
 *  - Public:   /api/health, /api/auth/register, /api/auth/login
 *  - Everything else needs a valid login token.
 *  - Role checks (e.g. "admins only") are written on each controller method
 *    with @PreAuthorize, which @EnableMethodSecurity switches on.
 */
@Configuration
@EnableMethodSecurity
public class SecurityConfig {

    private final JwtService jwtService;
    private final UserRepository userRepository;
    private final List<String> allowedOrigins;

    public SecurityConfig(JwtService jwtService,
                          UserRepository userRepository,
                          @Value("${app.cors.allowed-origins}") List<String> allowedOrigins) {
        this.jwtService = jwtService;
        this.userRepository = userRepository;
        this.allowedOrigins = allowedOrigins;
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
            // We use tokens, not cookies, so CSRF protection and server sessions are not needed
            .csrf(AbstractHttpConfigurer::disable)
            .sessionManagement(s -> s.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            .cors(cors -> cors.configurationSource(corsConfigurationSource()))
            .authorizeHttpRequests(auth -> auth
                .requestMatchers(HttpMethod.OPTIONS, "/**").permitAll()
                .requestMatchers("/api/health", "/api/auth/register", "/api/auth/login").permitAll()
                .anyRequest().authenticated())
            // 401 = not logged in, 403 = logged in but this role is not allowed
            .exceptionHandling(e -> e
                .authenticationEntryPoint((req, res, ex) -> writeError(res, HttpStatus.UNAUTHORIZED, "Please log in"))
                .accessDeniedHandler((req, res, ex) -> writeError(res, HttpStatus.FORBIDDEN, "You do not have access to this")))
            .httpBasic(AbstractHttpConfigurer::disable)
            .formLogin(AbstractHttpConfigurer::disable)
            .addFilterBefore(new JwtAuthenticationFilter(jwtService, userRepository),
                             UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }

    /** BCrypt turns "Demo@123" into a one-way hash like "$2a$10$oLVr...". */
    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    /** Which frontend addresses may call this API from a browser (CORS). */
    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration config = new CorsConfiguration();
        config.setAllowedOrigins(allowedOrigins);
        config.setAllowedMethods(List.of("GET", "POST", "PUT", "DELETE", "OPTIONS"));
        config.setAllowedHeaders(List.of("Authorization", "Content-Type"));
        config.setAllowCredentials(true);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", config);
        return source;
    }

    /** Same JSON shape as GlobalExceptionHandler, so the frontend handles every error the same way. */
    private static void writeError(HttpServletResponse res, HttpStatus status, String message) throws IOException {
        res.setStatus(status.value());
        res.setContentType(MediaType.APPLICATION_JSON_VALUE);
        res.getWriter().write("{\"status\":" + status.value()
                + ",\"message\":\"" + message + "\""
                + ",\"timestamp\":\"" + Instant.now() + "\"}");
    }
}
