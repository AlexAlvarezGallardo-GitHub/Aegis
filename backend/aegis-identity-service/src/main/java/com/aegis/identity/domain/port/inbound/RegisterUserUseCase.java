package com.aegis.identity.domain.port.inbound;

/**
 * Inbound port for registering a new user in the identity service.
 */
public interface RegisterUserUseCase {

    /**
     * Registers a new user described by the given command.
     *
     * @param command the registration request containing user data and correlation id
     * @return the registration result with the created user identity
     */
    Result register(Command command);

    /**
     * Command data for registering a new user.
     *
     * @param email         the user's email address
     * @param password      the user's raw password
     * @param firstName     the user's first name
     * @param lastName      the user's last name
     * @param correlationId the client correlation id for tracing the request
     */
    record Command(String email, String password, String firstName, String lastName, String correlationId) {
    }

    /**
     * Result data returned after a successful registration.
     *
     * @param userId      the unique identifier of the newly registered user
     * @param email       the registered email address
     * @param status      the initial user status
     * @param registeredAt the instant when the user was registered
     */
    record Result(java.util.UUID userId, String email, String status, java.time.Instant registeredAt) {
    }
}
