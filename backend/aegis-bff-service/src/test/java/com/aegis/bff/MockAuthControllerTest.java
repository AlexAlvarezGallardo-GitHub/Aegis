package com.aegis.bff;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class MockAuthControllerTest {

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        SessionJwtStore sessionJwtStore = new SessionJwtStore();
        sessionJwtStore.injectSession(new MockHttpSession());
        MockLoginService mockLoginService = new MockLoginService(sessionJwtStore);
        mockMvc = MockMvcBuilders.standaloneSetup(new MockAuthController(mockLoginService)).build();
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
}
