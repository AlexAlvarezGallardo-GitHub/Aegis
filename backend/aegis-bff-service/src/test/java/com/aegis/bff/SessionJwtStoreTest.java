package com.aegis.bff;

import jakarta.servlet.http.HttpSession;
import org.junit.jupiter.api.Test;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

class SessionJwtStoreTest {

    private final SessionJwtStore store = new SessionJwtStore();

    @Test
    void shouldStoreAndRetrieveTokens() {
        HttpSession session = new FakeSession();
        store.injectSession(session);

        store.storeTokens("access-123", "refresh-456");

        assertEquals(Optional.of("access-123"), store.getAccessToken());
        assertEquals(Optional.of("refresh-456"), store.getRefreshToken());
    }

    @Test
    void shouldReturnEmptyWhenNoSession() {
        assertTrue(store.getAccessToken().isEmpty());
        assertTrue(store.getRefreshToken().isEmpty());
    }

    @Test
    void shouldClearAndInvalidate() {
        HttpSession session = new FakeSession();
        store.injectSession(session);
        store.storeTokens("access", "refresh");

        FakeSession fakeSession = (FakeSession) session;
        store.clear();

        assertTrue(fakeSession.isInvalid());
    }

    @Test
    void shouldDoNothingOnClearWhenNoSession() {
        store.clear();
        assertTrue(store.getAccessToken().isEmpty());
    }

    static class FakeSession implements HttpSession {
        private final java.util.Map<String, Object> attrs = new java.util.HashMap<>();
        private boolean invalid = false;

        @Override
        public void setAttribute(String name, Object value) { attrs.put(name, value); }

        @Override
        public Object getAttribute(String name) { return attrs.get(name); }

        @Override
        public void removeAttribute(String name) { attrs.remove(name); }

        @Override
        public void invalidate() { this.invalid = true; }

        boolean isInvalid() { return invalid; }

        // Unused HttpSession methods
        public long getCreationTime() { return 0; }
        public String getId() { return "test"; }
        public long getLastAccessedTime() { return 0; }
        public jakarta.servlet.ServletContext getServletContext() { return null; }
        public void setMaxInactiveInterval(int interval) {}
        public int getMaxInactiveInterval() { return 0; }
        public java.util.Enumeration<String> getAttributeNames() { return null; }
        public boolean isNew() { return false; }
    }
}
