package com.aegis.identity.domain.port.inbound;

import com.aegis.identity.domain.model.TokenPair;

public interface AuthenticateUserUseCase {

    Result authenticate(Command command);

    record Command(String email, String password, String correlationId) {
    }

    record Result(TokenPair tokenPair, boolean emailVerified) {
    }
}
