package com.aegis.identity.infrastructure.security;

import com.aegis.identity.domain.model.TokenPair;
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
    private final long refreshTokenExpirationMs;

    public JwtTokenProvider(
            @Value("${aegis.jwt.secret}") String secret,
            @Value("${aegis.jwt.access-token-expiration-ms:900000}") long accessTokenExpirationMs,
            @Value("${aegis.jwt.refresh-token-expiration-ms:604800000}") long refreshTokenExpirationMs) {
        this.secretKey = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
        this.accessTokenExpirationMs = accessTokenExpirationMs;
        this.refreshTokenExpirationMs = refreshTokenExpirationMs;
    }

    @Override
    public TokenPair generateTokenPair(UserId userId, String email) {
        String accessToken = generateAccessToken(userId, email);
        String refreshToken = generateRefreshToken(userId, email);
        return new TokenPair(accessToken, refreshToken);
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
    public String generateRefreshToken(UserId userId, String email) {
        Date now = new Date();
        Date expiry = new Date(now.getTime() + refreshTokenExpirationMs);

        return Jwts.builder()
                .subject(userId.value().toString())
                .claim("email", email)
                .claim("type", "refresh")
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
    public UserId validateRefreshToken(String token) {
        try {
            Claims claims = Jwts.parser()
                    .verifyWith(secretKey)
                    .build()
                    .parseSignedClaims(token)
                    .getPayload();

            String tokenType = claims.get("type", String.class);
            if (!"refresh".equals(tokenType)) {
                throw new JwtException("Invalid token type: expected refresh");
            }

            return UserId.of(java.util.UUID.fromString(claims.getSubject()));
        } catch (JwtException | IllegalArgumentException e) {
            log.warn("Invalid refresh token: {}", e.getMessage());
            throw new io.jsonwebtoken.security.SecurityException("Invalid refresh token", e);
        }
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
