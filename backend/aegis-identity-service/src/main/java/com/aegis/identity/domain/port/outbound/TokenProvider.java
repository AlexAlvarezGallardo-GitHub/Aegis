package com.aegis.identity.domain.port.outbound;

import com.aegis.identity.domain.model.TokenPair;
import com.aegis.identity.domain.model.UserId;

public interface TokenProvider {

    TokenPair generateTokenPair(UserId userId, String email);

    String generateAccessToken(UserId userId, String email);

    String generateRefreshToken(UserId userId, String email);

    UserId validateAccessToken(String token);

    UserId validateRefreshToken(String token);
}
