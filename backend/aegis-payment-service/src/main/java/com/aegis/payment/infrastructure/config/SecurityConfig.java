package com.aegis.payment.infrastructure.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;

/**
 * Security configuration — JWT resource server in production, permit-all in dev.
 */
@Configuration
@EnableWebSecurity
public class SecurityConfig {

    /**
     * Production security filter chain — stateless, JWT-authenticated.
     *
     * @param http the HTTP security builder
     * @return the filter chain
     * @throws Exception if configuration fails
     */
    @Bean
    @Profile("!dev")
    public SecurityFilterChain productionSecurityFilterChain(HttpSecurity http) throws Exception {
        http
                .csrf(csrf -> csrf.disable())
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers(
                                "/actuator/health/**",
                                "/actuator/prometheus",
                                "/actuator/metrics/**",
                                "/actuator/info"
                        ).permitAll()
                        .anyRequest().authenticated()
                );
        return http.build();
    }

    /**
     * Dev security filter chain — permit all for local development.
     *
     * @param http the HTTP security builder
     * @return the filter chain
     * @throws Exception if configuration fails
     */
    @Bean
    @Profile("dev")
    public SecurityFilterChain devSecurityFilterChain(HttpSecurity http) throws Exception {
        http
                .csrf(csrf -> csrf.disable())
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> auth
                        .anyRequest().permitAll()
                );
        return http.build();
    }
}
