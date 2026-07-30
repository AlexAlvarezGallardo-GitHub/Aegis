package com.aegis.bff;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Base64;
import java.util.List;

public class SessionJwtAuthenticationFilter extends OncePerRequestFilter {

    private final SessionJwtStore sessionJwtStore;
    private final ObjectMapper objectMapper;

    public SessionJwtAuthenticationFilter(SessionJwtStore sessionJwtStore, ObjectMapper objectMapper) {
        this.sessionJwtStore = sessionJwtStore;
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
                String payload = new String(Base64.getUrlDecoder().decode(token.split("\\.")[1]));
                JsonNode claims = objectMapper.readTree(payload);

                String userId = claims.get("sub").asText();
                String email = claims.has("email") ? claims.get("email").asText() : "";

                UsernamePasswordAuthenticationToken auth =
                        new UsernamePasswordAuthenticationToken(userId, email, List.of());
                SecurityContextHolder.getContext().setAuthentication(auth);
            } catch (Exception ignored) {
            }
        });

        chain.doFilter(request, response);
    }
}
