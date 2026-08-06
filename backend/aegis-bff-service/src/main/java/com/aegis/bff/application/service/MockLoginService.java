package com.aegis.bff.application.service;

import com.aegis.bff.domain.port.JwtSigningKey;
import com.aegis.bff.domain.port.TokenStore;
import io.jsonwebtoken.Jwts;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.time.Instant;
import java.util.Date;
import java.util.Map;
import java.util.UUID;

/**
 * Development-only service that generates a signed JWT for local testing.
 *
 * <p>The mock token is signed with the same HMAC secret used by the Identity Service
 * so that the BFF's token validator accepts it without requiring a running
 * Identity Service.</p>
 */
@Profile("dev")
@Service
public class MockLoginService {

    private static final String MOCK_EMAIL = "mock@aegis.dev";
    private static final UUID MOCK_USER_ID = UUID.fromString("00000000-0000-0000-0000-000000000001");

    private final TokenStore tokenStore;
    private final SecretKey secretKey;

    public MockLoginService(TokenStore tokenStore, JwtSigningKey signingKey) {
        this.tokenStore = tokenStore;
        this.secretKey = signingKey.get();
    }

    public Map<String, Object> mockLogin() {
        Instant now = Instant.now();
        Date issuedAt = Date.from(now);
        Date accessExpiration = Date.from(now.plusSeconds(86400));
        Date refreshExpiration = Date.from(now.plusSeconds(86400));

        String accessToken = Jwts.builder()
                .subject(MOCK_USER_ID.toString())
                .claim("email", MOCK_EMAIL)
                .claim("type", "access")
                .issuedAt(issuedAt)
                .expiration(accessExpiration)
                .signWith(secretKey)
                .compact();

        String refreshToken = Jwts.builder()
                .subject(MOCK_USER_ID.toString())
                .claim("email", MOCK_EMAIL)
                .claim("type", "refresh")
                .issuedAt(issuedAt)
                .expiration(refreshExpiration)
                .signWith(secretKey)
                .compact();

        tokenStore.storeTokens(accessToken, refreshToken);

        return Map.of(
                "tokenType", "Bearer",
                "expiresIn", 86400L,
                "emailVerified", true,
                "mock", true
        );
    }

    public static UUID getMockUserId() {
        return MOCK_USER_ID;
    }
}
