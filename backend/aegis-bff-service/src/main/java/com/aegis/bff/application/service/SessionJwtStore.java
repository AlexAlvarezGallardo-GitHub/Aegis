package com.aegis.bff.application.service;

import com.aegis.bff.domain.port.TokenStore;
import jakarta.servlet.http.HttpSession;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.util.Optional;

/**
 * HTTP-session-backed implementation of {@link TokenStore}.
 *
 * <p>Reads the current {@link HttpSession} from Spring's {@link RequestContextHolder}
 * on every call, so there is no mutable state held inside this component.</p>
 */
@Component
public class SessionJwtStore implements TokenStore {

    private static final String ACCESS_TOKEN_KEY = "ACCESS_TOKEN";
    private static final String REFRESH_TOKEN_KEY = "REFRESH_TOKEN";

    @Override
    public void storeTokens(String accessToken, String refreshToken) {
        HttpSession session = getOrCreateSession();
        session.setAttribute(ACCESS_TOKEN_KEY, accessToken);
        session.setAttribute(REFRESH_TOKEN_KEY, refreshToken);
    }

    @Override
    public Optional<String> getAccessToken() {
        return Optional.ofNullable(getSession())
                .map(s -> (String) s.getAttribute(ACCESS_TOKEN_KEY));
    }

    @Override
    public Optional<String> getRefreshToken() {
        return Optional.ofNullable(getSession())
                .map(s -> (String) s.getAttribute(REFRESH_TOKEN_KEY));
    }

    @Override
    public void clear() {
        HttpSession session = getSession();
        if (session != null) {
            session.removeAttribute(ACCESS_TOKEN_KEY);
            session.removeAttribute(REFRESH_TOKEN_KEY);
            session.invalidate();
        }
    }

    private HttpSession getOrCreateSession() {
        ServletRequestAttributes attrs = (ServletRequestAttributes)
                RequestContextHolder.getRequestAttributes();
        if (attrs == null || attrs.getRequest() == null) {
            throw new IllegalStateException("No HTTP request context available");
        }
        return attrs.getRequest().getSession(true);
    }

    private HttpSession getSession() {
        ServletRequestAttributes attrs = (ServletRequestAttributes)
                RequestContextHolder.getRequestAttributes();
        if (attrs == null || attrs.getRequest() == null) {
            return null;
        }
        return attrs.getRequest().getSession(false);
    }
}
