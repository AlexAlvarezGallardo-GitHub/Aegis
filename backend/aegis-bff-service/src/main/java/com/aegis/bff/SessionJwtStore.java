package com.aegis.bff;

import jakarta.servlet.http.HttpSession;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.util.Optional;

@Component
public class SessionJwtStore {

    private static final String ACCESS_TOKEN_KEY = "ACCESS_TOKEN";
    private static final String REFRESH_TOKEN_KEY = "REFRESH_TOKEN";

    public void storeTokens(String accessToken, String refreshToken) {
        HttpSession session = getOrCreateSession();
        session.setAttribute(ACCESS_TOKEN_KEY, accessToken);
        session.setAttribute(REFRESH_TOKEN_KEY, refreshToken);
    }

    public Optional<String> getAccessToken() {
        return Optional.ofNullable(getSession())
                .map(s -> (String) s.getAttribute(ACCESS_TOKEN_KEY));
    }

    public Optional<String> getRefreshToken() {
        return Optional.ofNullable(getSession())
                .map(s -> (String) s.getAttribute(REFRESH_TOKEN_KEY));
    }

    public void clear() {
        HttpSession session = getSession();
        if (session != null) {
            session.removeAttribute(ACCESS_TOKEN_KEY);
            session.removeAttribute(REFRESH_TOKEN_KEY);
            session.invalidate();
        }
    }

    private HttpSession getOrCreateSession() {
        if (sessionOverride != null) {
            return sessionOverride;
        }
        ServletRequestAttributes attrs = (ServletRequestAttributes)
                RequestContextHolder.getRequestAttributes();
        if (attrs == null || attrs.getRequest() == null) {
            throw new IllegalStateException("No HTTP request context available");
        }
        return attrs.getRequest().getSession(true);
    }

    private HttpSession getSession() {
        if (sessionOverride != null) {
            return sessionOverride;
        }
        ServletRequestAttributes attrs = (ServletRequestAttributes)
                RequestContextHolder.getRequestAttributes();
        if (attrs == null || attrs.getRequest() == null) {
            return null;
        }
        return attrs.getRequest().getSession(false);
    }

    private HttpSession sessionOverride;

    void injectSession(HttpSession session) {
        this.sessionOverride = session;
    }
}
