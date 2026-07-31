package com.aegis.bff;

import com.aegis.bff.application.service.SessionJwtStore;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

class SessionJwtStoreTest {

    private SessionJwtStore store;
    private MockHttpServletRequest request;

    @BeforeEach
    void setUp() {
        store = new SessionJwtStore();
        request = new MockHttpServletRequest();
        RequestContextHolder.setRequestAttributes(new ServletRequestAttributes(request));
    }

    @AfterEach
    void tearDown() {
        RequestContextHolder.resetRequestAttributes();
    }

    @Test
    void shouldStoreAndRetrieveTokens() {
        store.storeTokens("access-123", "refresh-456");

        assertEquals(Optional.of("access-123"), store.getAccessToken());
        assertEquals(Optional.of("refresh-456"), store.getRefreshToken());
    }

    @Test
    void shouldReturnEmptyWhenNoSession() {
        // Reset request attributes so there is no session context
        RequestContextHolder.resetRequestAttributes();
        assertTrue(store.getAccessToken().isEmpty());
        assertTrue(store.getRefreshToken().isEmpty());
    }

    @Test
    void shouldClearAndInvalidate() {
        store.storeTokens("access", "refresh");
        store.clear();

        assertTrue(store.getAccessToken().isEmpty());
        assertTrue(store.getRefreshToken().isEmpty());
    }

    @Test
    void shouldDoNothingOnClearWhenNoSession() {
        RequestContextHolder.resetRequestAttributes();
        store.clear();
        assertTrue(store.getAccessToken().isEmpty());
    }

    @Test
    void shouldThrowWhenStoringTokensWithoutRequestContext() {
        RequestContextHolder.resetRequestAttributes();
        IllegalStateException ex = assertThrows(IllegalStateException.class,
                () -> store.storeTokens("access", "refresh"));
        assertTrue(ex.getMessage().contains("No HTTP request context available"));
    }
}
