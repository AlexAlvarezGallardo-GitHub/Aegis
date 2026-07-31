package com.aegis.identity.application.service;

import com.aegis.identity.domain.exception.AccountLockedException;
import com.aegis.identity.domain.exception.AccountSuspendedException;
import com.aegis.identity.domain.exception.InvalidCredentialsException;
import com.aegis.identity.domain.model.TokenPair;
import com.aegis.identity.domain.model.UserId;
import com.aegis.identity.domain.port.inbound.RefreshTokenUseCase;
import com.aegis.identity.domain.port.outbound.RefreshTokenRepository;
import com.aegis.identity.domain.port.outbound.RefreshTokenRepository.StoredRefreshToken;
import com.aegis.identity.domain.port.outbound.TokenProvider;
import com.aegis.identity.domain.port.outbound.UserRepository;
import com.aegis.identity.domain.model.UserStatus;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Instant;
import java.util.HexFormat;
import java.util.UUID;

/**
 * Application service implementing refresh token rotation.
 *
 * <p>On each refresh, the old token is revoked and a new opaque token is generated,
 * ensuring that refresh tokens cannot be reused (rotation).</p>
 */
@Service
public class RefreshTokenService implements RefreshTokenUseCase {

    private final TokenProvider tokenProvider;
    private final UserRepository userRepository;
    private final RefreshTokenRepository refreshTokenRepository;
    private final long refreshTokenExpirationMs;

    public RefreshTokenService(TokenProvider tokenProvider,
                                UserRepository userRepository,
                                RefreshTokenRepository refreshTokenRepository,
                                @Value("${aegis.jwt.refresh-token-expiration-ms:604800000}") long refreshTokenExpirationMs) {
        this.tokenProvider = tokenProvider;
        this.userRepository = userRepository;
        this.refreshTokenRepository = refreshTokenRepository;
        this.refreshTokenExpirationMs = refreshTokenExpirationMs;
    }

    @Override
    @Transactional
    public Result refresh(Command command) {
        String tokenHash = hashToken(command.refreshToken());

        StoredRefreshToken storedToken = refreshTokenRepository.findByTokenHash(tokenHash)
                .orElseThrow(InvalidCredentialsException::new);

        if (storedToken.revokedAt() != null) {
            throw new InvalidCredentialsException();
        }
        if (Instant.now().isAfter(storedToken.expiresAt())) {
            throw new InvalidCredentialsException();
        }

        UserId userId = storedToken.userId();

        var user = userRepository.findById(userId)
                .orElseThrow(InvalidCredentialsException::new);

        if (user.getStatus() == UserStatus.LOCKED) {
            throw new AccountLockedException();
        }
        if (user.getStatus() == UserStatus.SUSPENDED) {
            throw new AccountSuspendedException();
        }

        refreshTokenRepository.revoke(tokenHash);

        String newAccessToken = tokenProvider.generateAccessToken(userId, user.getEmail().value());
        String newRefreshTokenValue = generateOpaqueToken();
        String newRefreshTokenHash = hashToken(newRefreshTokenValue);

        Instant expiresAt = Instant.now().plusMillis(refreshTokenExpirationMs);
        StoredRefreshToken newStoredToken = new StoredRefreshToken(
                UUID.randomUUID(),
                newRefreshTokenHash,
                userId,
                expiresAt,
                null,
                Instant.now()
        );
        refreshTokenRepository.save(newStoredToken);

        TokenPair tokenPair = new TokenPair(newAccessToken, newRefreshTokenValue);
        return new Result(tokenPair);
    }

    private static String hashToken(String token) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(token.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(hash);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 algorithm not available", e);
        }
    }

    private static String generateOpaqueToken() {
        return UUID.randomUUID().toString().replace("-", "")
                + UUID.randomUUID().toString().replace("-", "");
    }
}
