package com.aegis.identity.infrastructure.security;

import com.aegis.identity.domain.model.UserId;
import com.aegis.identity.domain.port.outbound.TokenProvider;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;

@Component
public class JwtTokenProvider implements TokenProvider {

    private static final Logger log = LoggerFactory.getLogger(JwtTokenProvider.class);

    private final SecretKey secretKey;
    private final long accessTokenExpirationMs;

    public JwtTokenProvider(
            @Value("${aegis.jwt.secret}") String secret,
            @Value("${aegis.jwt.access-token-expiration-ms:900000}") long accessTokenExpirationMs) {
        this.secretKey = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
        this.accessTokenExpirationMs = accessTokenExpirationMs;
    }

    @Override
    public String generateAccessToken(UserId userId, String email) {
        Date now = new Date();
        Date expiry = new Date(now.getTime() + accessTokenExpirationMs);

        return Jwts.builder()
                .subject(userId.value().toString())
                .claim("email", email)
                .claim("type", "access")
                .issuedAt(now)
                .expiration(expiry)
                .signWith(secretKey)
                .compact();
    }

    @Override
    public UserId validateAccessToken(String token) {
        try {
            Claims claims = Jwts.parser()
                    .verifyWith(secretKey)
                    .build()
                    .parseSignedClaims(token)
                    .getPayload();

            String tokenType = claims.get("type", String.class);
            if (!"access".equals(tokenType)) {
                throw new JwtException("Invalid token type: expected access");
            }

            return UserId.of(java.util.UUID.fromString(claims.getSubject()));
        } catch (JwtException | IllegalArgumentException e) {
            log.warn("Invalid access token: {}", e.getMessage());
            throw new io.jsonwebtoken.security.SecurityException("Invalid access token", e);
        }
    }

    @Override
    public long getAccessTokenExpirySeconds() {
        return accessTokenExpirationMs / 1000;
    }

    public boolean validateToken(String token) {
        try {
            validateAccessToken(token);
            return true;
        } catch (Exception e) {
            return false;
        }
    }
}
