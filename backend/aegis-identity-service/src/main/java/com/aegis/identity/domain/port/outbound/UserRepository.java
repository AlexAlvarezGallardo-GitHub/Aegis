package com.aegis.identity.domain.port.outbound;

import com.aegis.identity.domain.model.Email;
import com.aegis.identity.domain.model.User;
import com.aegis.identity.domain.model.UserId;

import java.util.Optional;

/**
 * Outbound port for persisting and querying user aggregates.
 */
public interface UserRepository {

    /**
     * Persists the given user aggregate.
     *
     * @param user the user aggregate to save
     * @return the saved user aggregate
     */
    User save(User user);

    /**
     * Persists the given user aggregate and flushes the persistence context.
     *
     * @param user the user aggregate to save
     * @return the saved user aggregate
     */
    User saveAndFlush(User user);

    /**
     * Finds a user by its email address.
     *
     * @param email the email value object
     * @return an optional containing the user if found
     */
    Optional<User> findByEmail(Email email);

    /**
     * Finds a user by its unique identifier.
     *
     * @param userId the user identifier
     * @return an optional containing the user if found
     */
    Optional<User> findById(UserId userId);

    /**
     * Checks whether a user with the given email already exists.
     *
     * @param email the email value object
     * @return true if a user with the email exists, false otherwise
     */
    boolean existsByEmail(Email email);
}
