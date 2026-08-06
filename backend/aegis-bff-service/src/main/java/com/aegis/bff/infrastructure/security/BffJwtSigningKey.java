package com.aegis.bff.infrastructure.security;

import com.aegis.bff.domain.port.JwtSigningKey;
import com.aegis.bff.infrastructure.config.BffProperties;
import io.jsonwebtoken.security.Keys;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;

/**
 * Provides the HMAC signing key bound from {@link BffProperties}.
 */
@Component
public class BffJwtSigningKey implements JwtSigningKey {

    private final SecretKey secretKey;

    public BffJwtSigningKey(BffProperties properties) {
        this.secretKey = Keys.hmacShaKeyFor(
                properties.jwt().secret().getBytes(StandardCharsets.UTF_8));
    }

    @Override
    public SecretKey get() {
        return secretKey;
    }
}
