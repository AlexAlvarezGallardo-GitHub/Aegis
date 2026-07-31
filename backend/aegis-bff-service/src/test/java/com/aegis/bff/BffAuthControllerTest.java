package com.aegis.bff;

import com.aegis.bff.application.service.BffService;
import com.aegis.bff.web.controller.BffAuthController;
import com.aegis.bff.web.dto.LoginRequest;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.util.Map;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

class BffAuthControllerTest {

    private final ObjectMapper objectMapper = new ObjectMapper();
    private FakeBffService fakeBffService;
    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        fakeBffService = new FakeBffService();
        mockMvc = MockMvcBuilders.standaloneSetup(new BffAuthController(fakeBffService)).build();
    }

    @Test
    void shouldLogin() throws Exception {
        fakeBffService.loginResult = Map.of(
                "tokenType", "Bearer",
                "expiresIn", 900,
                "emailVerified", true
        );

        mockMvc.perform(post("/api/bff/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                Map.of("email", "user@test.com", "password", "pass"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.tokenType").value("Bearer"))
                .andExpect(jsonPath("$.expiresIn").value(900))
                .andExpect(jsonPath("$.emailVerified").value(true));
    }

    @Test
    void shouldLogout() throws Exception {
        mockMvc.perform(post("/api/bff/auth/logout"))
                .andExpect(status().isNoContent());
    }

    @Test
    void shouldGetCurrentUser() throws Exception {
        fakeBffService.currentUserResult = Map.of("userId", "uuid-123", "email", "john@example.com");

        mockMvc.perform(get("/api/bff/auth/me"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.userId").value("uuid-123"))
                .andExpect(jsonPath("$.email").value("john@example.com"));
    }

    @Test
    void shouldRefresh() throws Exception {
        fakeBffService.refreshResult = Map.of("tokenType", "Bearer", "expiresIn", 900);

        mockMvc.perform(post("/api/bff/auth/refresh"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.tokenType").value("Bearer"))
                .andExpect(jsonPath("$.expiresIn").value(900));
    }

    @Test
    void shouldLoginWithoutCorrelationIdHeader() throws Exception {
        fakeBffService.loginResult = Map.of(
                "tokenType", "Bearer",
                "expiresIn", 900,
                "emailVerified", false
        );

        mockMvc.perform(post("/api/bff/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                Map.of("email", "user@test.com", "password", "pass"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.tokenType").value("Bearer"))
                .andExpect(jsonPath("$.emailVerified").value(false));
    }

    @Test
    void shouldRefreshWithoutCorrelationIdHeader() throws Exception {
        fakeBffService.refreshResult = Map.of("tokenType", "Bearer", "expiresIn", 600);

        mockMvc.perform(post("/api/bff/auth/refresh"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.expiresIn").value(600));
    }

    @Test
    void shouldLoginWithExplicitCorrelationId() throws Exception {
        fakeBffService.loginResult = Map.of(
                "tokenType", "Bearer",
                "expiresIn", 900,
                "emailVerified", true
        );

        mockMvc.perform(post("/api/bff/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("X-Correlation-Id", "explicit-corr-id")
                        .content(objectMapper.writeValueAsString(
                                Map.of("email", "user@test.com", "password", "pass"))))
                .andExpect(status().isOk());
    }

    /**
     * Test double that overrides all BffService methods to return preconfigured results.
     */
    static class FakeBffService extends BffService {

        Map<String, Object> loginResult;
        Map<String, Object> refreshResult;
        Map<String, Object> currentUserResult;

        FakeBffService() {
            super(null, null, null);
        }

        @Override
        public Map<String, Object> login(String email, String password, String correlationId) {
            return loginResult;
        }

        @Override
        public Map<String, Object> refresh(String correlationId) {
            return refreshResult;
        }

        @Override
        public void logout() {
        }

        @Override
        public Map<String, Object> getCurrentUser() {
            if (currentUserResult == null) throw new IllegalStateException("Not authenticated");
            return currentUserResult;
        }
    }
}
