package com.aegis.identity.domain.port.inbound;

/**
 * Inbound port for authenticating a registered user with email and password.
 */
public interface AuthenticateUserUseCase {

    /**
     * Authenticates the user described by the given command.
     *
     * @param command the authentication request containing email, password and correlation id
     * @return the authentication result with the generated access token
     */
    Result authenticate(Command command);

    /**
     * Command data for authenticating a user.
     *
     * @param email          the user's email address
     * @param password       the user's raw password
     * @param correlationId  the client correlation id for tracing the request
     */
    record Command(String email, String password, String correlationId) {
    }

    /**
     * Result data returned after a successful authentication.
     *
     * @param accessToken   the JWT access token
     * @param emailVerified whether the user's email has been verified
     */
    record Result(String accessToken, boolean emailVerified) {
    }
}
