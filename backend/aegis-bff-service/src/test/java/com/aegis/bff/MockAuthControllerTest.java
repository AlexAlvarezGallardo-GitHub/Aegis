package com.aegis.bff;

import com.aegis.bff.application.service.MockLoginService;
import com.aegis.bff.application.service.SessionJwtStore;
import com.aegis.bff.domain.port.JwtSigningKey;
import com.aegis.bff.domain.port.TokenStore;
import com.aegis.bff.web.controller.MockAuthController;
import io.jsonwebtoken.security.Keys;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.nio.charset.StandardCharsets;
import java.util.Optional;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class MockAuthControllerTest {

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        // Use an in-memory TokenStore so we don't need a real HttpSession
        TokenStore tokenStore = new InMemoryTokenStore();
        JwtSigningKey signingKey = () -> Keys.hmacShaKeyFor(
                "aegis-dev-secret-key-that-is-at-least-256-bits-long-for-hs256-algorithm"
                        .getBytes(StandardCharsets.UTF_8));
        MockLoginService mockLoginService = new MockLoginService(tokenStore, signingKey);
        mockMvc = MockMvcBuilders.standaloneSetup(new MockAuthController(mockLoginService)).build();
    }

    @AfterEach
    void tearDown() {
        RequestContextHolder.resetRequestAttributes();
    }

    @Test
    void shouldReturnMockLoginResponse() throws Exception {
        mockMvc.perform(post("/api/bff/auth/mock-login"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.mock").value(true))
                .andExpect(jsonPath("$.tokenType").value("Bearer"))
                .andExpect(jsonPath("$.emailVerified").value(true));
    }

    @Test
    void shouldReturnExpiresIn() throws Exception {
        mockMvc.perform(post("/api/bff/auth/mock-login"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.expiresIn").value(86400));
    }

    /**
     * Minimal in-memory TokenStore for tests.
     */
    static class InMemoryTokenStore implements TokenStore {
        private String accessToken;
        private String refreshToken;

        @Override
        public void storeTokens(String accessToken, String refreshToken) {
            this.accessToken = accessToken;
            this.refreshToken = refreshToken;
        }

        @Override
        public Optional<String> getAccessToken() {
            return Optional.ofNullable(accessToken);
        }

        @Override
        public Optional<String> getRefreshToken() {
            return Optional.ofNullable(refreshToken);
        }

        @Override
        public void clear() {
            this.accessToken = null;
            this.refreshToken = null;
        }
    }
}
