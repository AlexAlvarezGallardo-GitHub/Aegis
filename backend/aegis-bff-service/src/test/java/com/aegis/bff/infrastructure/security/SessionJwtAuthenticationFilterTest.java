package com.aegis.bff.infrastructure.security;

import com.aegis.bff.application.service.SessionJwtStore;
import com.aegis.bff.domain.port.TokenValidator;
import com.aegis.bff.infrastructure.config.BffProperties;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import jakarta.servlet.FilterChain;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.core.context.SecurityContextHolder;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Date;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@DisplayName("SessionJwtAuthenticationFilter")
class SessionJwtAuthenticationFilterTest {

    private static final String TEST_SECRET = "aegis-dev-secret-key-that-is-at-least-256-bits-long-for-hs256-algorithm";
    private static final SecretKey SECRET_KEY = Keys.hmacShaKeyFor(TEST_SECRET.getBytes(StandardCharsets.UTF_8));

    private FakeSessionJwtStore sessionJwtStore;
    private TokenValidator tokenValidator;
    private SessionJwtAuthenticationFilter filter;
    private MockHttpServletRequest request;
    private MockHttpServletResponse response;
    private FilterChain filterChain;

    @BeforeEach
    void setUp() {
        SecurityContextHolder.clearContext();
        sessionJwtStore = new FakeSessionJwtStore(null);
        BffProperties props = new BffProperties(
                new BffProperties.ServiceUrl("http://localhost"),
                new BffProperties.ServiceUrl("http://localhost"),
                new BffProperties.Jwt(TEST_SECRET)
        );
        tokenValidator = new JwtTokenValidator(props);
        filter = new SessionJwtAuthenticationFilter(sessionJwtStore, tokenValidator, new ObjectMapper());
        request = new MockHttpServletRequest();
        response = new MockHttpServletResponse();
        filterChain = mock(FilterChain.class);
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Nested
    @DisplayName("When valid token is present in session")
    class WhenValidTokenPresent {

        @Test
        @DisplayName("Should set SecurityContext with authenticated principal")
        void shouldSetSecurityContext() throws Exception {
            // Arrange
            Instant now = Instant.now();
            String token = Jwts.builder()
                    .subject("user-uuid")
                    .claim("email", "john@example.com")
                    .claim("type", "access")
                    .issuedAt(Date.from(now))
                    .expiration(Date.from(now.plusSeconds(3600)))
                    .signWith(SECRET_KEY)
                    .compact();
            sessionJwtStore.setAccessToken(token);

            // Act
            filter.doFilterInternal(request, response, filterChain);

            // Assert
            var auth = SecurityContextHolder.getContext().getAuthentication();
            assertNotNull(auth);
            assertEquals("user-uuid", auth.getPrincipal());
            assertEquals("john@example.com", auth.getCredentials());
            assertTrue(auth.isAuthenticated());
            verify(filterChain).doFilter(request, response);
        }

        @Test
        @DisplayName("Should use empty string when email claim is missing")
        void shouldUseEmptyEmailWhenMissing() throws Exception {
            // Arrange
            Instant now = Instant.now();
            String token = Jwts.builder()
                    .subject("user-uuid")
                    .claim("type", "access")
                    .issuedAt(Date.from(now))
                    .expiration(Date.from(now.plusSeconds(3600)))
                    .signWith(SECRET_KEY)
                    .compact();
            sessionJwtStore.setAccessToken(token);

            // Act
            filter.doFilterInternal(request, response, filterChain);

            // Assert
            var auth = SecurityContextHolder.getContext().getAuthentication();
            assertNotNull(auth);
            assertEquals("user-uuid", auth.getPrincipal());
            assertEquals("", auth.getCredentials());
        }
    }

    @Nested
    @DisplayName("When no token is present in session")
    class WhenNoTokenPresent {

        @Test
        @DisplayName("Should not set SecurityContext")
        void shouldNotSetSecurityContext() throws Exception {
            // Arrange
            sessionJwtStore.setAccessToken(null);

            // Act
            filter.doFilterInternal(request, response, filterChain);

            // Assert
            var auth = SecurityContextHolder.getContext().getAuthentication();
            assertNull(auth);
            verify(filterChain).doFilter(request, response);
        }
    }

    @Nested
    @DisplayName("When token validation fails")
    class WhenTokenValidationFails {

        @Test
        @DisplayName("Should not set SecurityContext on invalid token")
        void shouldNotSetSecurityContextOnInvalidToken() throws Exception {
            // Arrange - token with wrong type
            Instant now = Instant.now();
            String token = Jwts.builder()
                    .subject("user-uuid")
                    .claim("type", "refresh")
                    .issuedAt(Date.from(now))
                    .expiration(Date.from(now.plusSeconds(3600)))
                    .signWith(SECRET_KEY)
                    .compact();
            sessionJwtStore.setAccessToken(token);

            // Act
            filter.doFilterInternal(request, response, filterChain);

            // Assert
            var auth = SecurityContextHolder.getContext().getAuthentication();
            assertNull(auth);
            verify(filterChain).doFilter(request, response);
        }
    }

    @Nested
    @DisplayName("When authentication already exists")
    class WhenAuthAlreadyExists {

        @Test
        @DisplayName("Should skip validation and continue filter chain")
        void shouldSkipValidation() throws Exception {
            // Arrange
            var existingAuth = new org.springframework.security.authentication
                    .UsernamePasswordAuthenticationToken("existing-user", null, java.util.List.of());
            SecurityContextHolder.getContext().setAuthentication(existingAuth);

            // Act
            filter.doFilterInternal(request, response, filterChain);

            // Assert
            assertNull(sessionJwtStore.getStoredToken());
            verify(filterChain).doFilter(request, response);
        }
    }

    /**
     * Test double for SessionJwtStore.
     */
    static class FakeSessionJwtStore extends SessionJwtStore {
        private String storedToken;

        FakeSessionJwtStore(String storedToken) {
            this.storedToken = storedToken;
        }

        void setAccessToken(String token) {
            this.storedToken = token;
        }

        String getStoredToken() {
            return storedToken;
        }

        @Override
        public Optional<String> getAccessToken() {
            return Optional.ofNullable(storedToken);
        }

        @Override
        public Optional<String> getRefreshToken() {
            return Optional.empty();
        }

        @Override
        public void storeTokens(String accessToken, String refreshToken) {
            this.storedToken = accessToken;
        }

        @Override
        public void clear() {
            this.storedToken = null;
        }
    }
}
