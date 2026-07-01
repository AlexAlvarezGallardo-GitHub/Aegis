package com.aegis.identity.domain.port.inbound;

public interface RegisterUserUseCase {

    Result register(Command command);

    record Command(String email, String password, String firstName, String lastName, String correlationId) {
    }

    record Result(java.util.UUID userId, String email, String status, java.time.Instant registeredAt) {
    }
}
