package com.aegis.bff.infrastructure.security;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.security.web.csrf.CookieCsrfTokenRepository;
import org.springframework.security.web.csrf.CsrfTokenRequestAttributeHandler;

/**
 * Security configuration for the BFF.
 *
 * <p>CSRF is enabled using a cookie-based token repository so that the Angular SPA
 * can read the token and send it back on state-changing requests (BREACH-safe pattern).</p>
 */
@Configuration
@EnableWebSecurity
public class SecurityConfig {

    private final SessionJwtAuthenticationFilter sessionJwtAuthenticationFilter;

    public SecurityConfig(SessionJwtAuthenticationFilter sessionJwtAuthenticationFilter) {
        this.sessionJwtAuthenticationFilter = sessionJwtAuthenticationFilter;
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        CsrfTokenRequestAttributeHandler csrfHandler = new CsrfTokenRequestAttributeHandler();
        // BREACH-safe: defer CSRF attribute resolution to render time
        csrfHandler.setCsrfRequestAttributeName(null);

        http
                .csrf(csrf -> csrf
                        .csrfTokenRepository(CookieCsrfTokenRepository.withHttpOnlyFalse())
                        .csrfTokenRequestHandler(csrfHandler::handle)
                        .ignoringRequestMatchers(
                                "/api/bff/auth/login",
                                "/api/bff/auth/refresh",
                                "/api/bff/auth/mock-login"
                        )
                )
                .sessionManagement(session -> session
                        .sessionCreationPolicy(SessionCreationPolicy.IF_REQUIRED)
                )
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers(
                                "/actuator/health/**"
                        ).permitAll()
                        .requestMatchers(
                                "/api/bff/auth/login",
                                "/api/bff/auth/refresh",
                                "/api/bff/auth/mock-login",
                                "/swagger-ui.html",
                                "/swagger-ui/**",
                                "/v3/api-docs/**",
                                "/webjars/**"
                        ).permitAll()
                        .anyRequest().authenticated()
                )
                .addFilterBefore(sessionJwtAuthenticationFilter,
                        UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }
}
