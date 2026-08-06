package com.aegis.bff.domain.port;

import javax.crypto.SecretKey;

/**
 * Port that provides the HMAC signing key used to issue JWTs.
 *
 * <p>Implemented by an infrastructure adapter that reads the configured secret,
 * keeping the application layer free of framework and configuration imports.</p>
 */
public interface JwtSigningKey {

    /**
     * @return the secret key used to sign and verify JWTs
     */
    SecretKey get();
}
