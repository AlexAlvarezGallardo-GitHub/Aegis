package com.aegis.identity.domain.port.inbound;

import com.aegis.identity.domain.model.TokenPair;

public interface RefreshTokenUseCase {

    Result refresh(Command command);

    record Command(String refreshToken, String correlationId) {
    }

    record Result(TokenPair tokenPair) {
    }
}
