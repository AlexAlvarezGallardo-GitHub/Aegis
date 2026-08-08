package com.aegis.identity.infrastructure.persistence;

import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;

import java.util.Optional;
import java.util.UUID;

/**
 * Spring Data JPA repository for user entities.
 */
public interface UserJpaRepository extends JpaRepository<UserJpaEntity, UUID> {

    /**
     * Checks whether a user with the given email exists.
     *
     * @param email the email address to check
     * @return true if a user with the email exists, false otherwise
     */
    boolean existsByEmail(String email);

    /**
     * Finds a user by its email address.
     *
     * <p>Uses a pessimistic write lock so that concurrent logins for the same
     * user serialize on the row instead of failing with an optimistic-lock
     * conflict ({@code StaleObjectStateException}) when updating failed-attempt
     * counters / last-login state.</p>
     *
     * @param email the email address to search for
     * @return an optional containing the user entity if found
     */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    Optional<UserJpaEntity> findByEmail(String email);
}
