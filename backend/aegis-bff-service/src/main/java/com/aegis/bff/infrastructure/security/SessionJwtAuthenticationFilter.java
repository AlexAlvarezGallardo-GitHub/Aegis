package com.aegis.bff.infrastructure.security;

import com.aegis.bff.application.service.SessionJwtStore;
import com.aegis.bff.domain.port.TokenValidator;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;

/**
 * Extracts the JWT from the session and, if valid, populates the Spring Security context.
 *
 * <p>Delegates signature verification to {@link TokenValidator} instead of decoding the
 * payload with raw Base64.</p>
 */
@Component
public class SessionJwtAuthenticationFilter extends OncePerRequestFilter {

    private static final Logger log = LoggerFactory.getLogger(SessionJwtAuthenticationFilter.class);

    private final SessionJwtStore sessionJwtStore;
    private final TokenValidator tokenValidator;
    private final ObjectMapper objectMapper;

    public SessionJwtAuthenticationFilter(SessionJwtStore sessionJwtStore,
                                          TokenValidator tokenValidator,
                                          ObjectMapper objectMapper) {
        this.sessionJwtStore = sessionJwtStore;
        this.tokenValidator = tokenValidator;
        this.objectMapper = objectMapper;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain chain) throws ServletException, IOException {

        Authentication existingAuth = SecurityContextHolder.getContext().getAuthentication();
        if (existingAuth != null && existingAuth.isAuthenticated()) {
            chain.doFilter(request, response);
            return;
        }

        sessionJwtStore.getAccessToken().ifPresent(token -> {
            try {
                Claims claims = tokenValidator.validate(token);
                String userId = claims.getSubject();
                String email = claims.get("email", String.class) != null
                        ? claims.get("email", String.class) : "";

                UsernamePasswordAuthenticationToken auth =
                        new UsernamePasswordAuthenticationToken(userId, email, List.of());
                SecurityContextHolder.getContext().setAuthentication(auth);
            } catch (JwtException ex) {
                log.debug("Rejecting invalid JWT from session: {}", ex.getMessage());
            } catch (Exception ex) {
                log.debug("Unexpected error while validating session JWT", ex);
            }
        });

        chain.doFilter(request, response);
    }
}
