package com.aegis.identity.infrastructure.persistence;

import org.springframework.data.jpa.repository.JpaRepository;

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
     * @param email the email address to search for
     * @return an optional containing the user entity if found
     */
    Optional<UserJpaEntity> findByEmail(String email);
}
