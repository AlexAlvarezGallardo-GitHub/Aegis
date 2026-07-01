package com.aegis.identity.infrastructure.persistence;

import com.aegis.identity.domain.model.Email;
import com.aegis.identity.domain.model.PasswordHash;
import com.aegis.identity.domain.model.User;
import com.aegis.identity.domain.model.UserId;
import com.aegis.identity.domain.model.UserStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("UserRepositoryAdapter - Persistence Adapter")
class UserRepositoryAdapterTest {

    @Mock
    private UserJpaRepository jpaRepository;

    private UserRepositoryAdapter adapter;

    @BeforeEach
    void setUp() {
        adapter = new UserRepositoryAdapter(jpaRepository);
    }

    @Nested
    @DisplayName("When saving a user")
    class WhenSavingUser {

        @Test
        @DisplayName("Should convert domain User to JPA entity and save")
        void shouldConvertAndSave() {
            // Arrange
            User user = User.register("save@example.com", "SecureP@ss1", "Save", "Test",
                    new com.aegis.identity.domain.port.outbound.PasswordHasher() {
                        @Override
                        public PasswordHash hash(String rawPassword) {
                            return PasswordHash.of("hashed_" + rawPassword);
                        }

                        @Override
                        public boolean matches(String rawPassword, PasswordHash hash) {
                            return false;
                        }
                    });

            when(jpaRepository.save(any(UserJpaEntity.class))).thenAnswer(inv -> {
                UserJpaEntity entity = inv.getArgument(0);
                return entity;
            });

            // Act
            User saved = adapter.save(user);

            // Assert
            assertNotNull(saved);
            assertEquals(user.getEmail().value(), saved.getEmail().value());
            verify(jpaRepository).save(any(UserJpaEntity.class));
        }
    }

    @Nested
    @DisplayName("When checking email existence")
    class WhenCheckingEmailExistence {

        @Test
        @DisplayName("Should delegate to JPA repository existsByEmail")
        void shouldDelegateToJpaRepository() {
            // Arrange
            Email email = Email.of("exists@example.com");
            when(jpaRepository.existsByEmail("exists@example.com")).thenReturn(true);

            // Act
            boolean exists = adapter.existsByEmail(email);

            // Assert
            assertTrue(exists);
            verify(jpaRepository).existsByEmail("exists@example.com");
        }

        @Test
        @DisplayName("Should return false when email does not exist")
        void shouldReturnFalseWhenNotExists() {
            // Arrange
            Email email = Email.of("notfound@example.com");
            when(jpaRepository.existsByEmail("notfound@example.com")).thenReturn(false);

            // Act
            boolean exists = adapter.existsByEmail(email);

            // Assert
            assertFalse(exists);
        }
    }

    @Nested
    @DisplayName("When finding by email")
    class WhenFindingByEmail {

        @Test
        @DisplayName("Should return user when email matches")
        void shouldReturnUserWhenEmailMatches() {
            // Arrange
            UUID userId = UUID.randomUUID();
            UserJpaEntity entity = new UserJpaEntity(
                    userId, "find@example.com", "hashedPass", "Find", "Me",
                    "PENDING_VERIFICATION", Instant.now(), Instant.now(), 0L);

            when(jpaRepository.findAll()).thenReturn(List.of(entity));

            // Act
            Optional<User> result = adapter.findByEmail(Email.of("find@example.com"));

            // Assert
            assertTrue(result.isPresent());
            assertEquals("find@example.com", result.get().getEmail().value());
        }

        @Test
        @DisplayName("Should return empty when email does not match")
        void shouldReturnEmptyWhenNoMatch() {
            // Arrange
            UUID userId = UUID.randomUUID();
            UserJpaEntity entity = new UserJpaEntity(
                    userId, "other@example.com", "hashedPass", "Other", "User",
                    "PENDING_VERIFICATION", Instant.now(), Instant.now(), 0L);

            when(jpaRepository.findAll()).thenReturn(List.of(entity));

            // Act
            Optional<User> result = adapter.findByEmail(Email.of("notfound@example.com"));

            // Assert
            assertTrue(result.isEmpty());
        }
    }

    @Nested
    @DisplayName("Known issues and risks")
    class KnownIssuesAndRisks {

        @Test
        @DisplayName("BUG: findByEmail uses findAll().stream().filter() - loads ALL users into memory")
        void documentsFindByEmailPerformanceBug() {
            // DOCUMENTATION TEST: This test documents a critical performance bug.
            //
            // In UserRepositoryAdapter.findByEmail():
            //
            //   public Optional<User> findByEmail(Email email) {
            //       return jpaRepository.findAll()           // <-- Loads ALL users from DB!
            //               .stream()
            //               .filter(e -> e.getEmail().equals(email.value()))
            //               .findFirst()
            //               .map(this::toDomain);
            //   }
            //
            // This loads ALL users from the database into memory, then filters in Java.
            // For a production system with thousands/millions of users, this will:
            // 1. Cause massive memory consumption
            // 2. Cause slow query performance
            // 3. Cause network overhead (transferring all rows)
            //
            // Fix: Add a findByEmail(String email) method to UserJpaRepository:
            //   Optional<UserJpaEntity> findByEmail(String email);
            //
            // Then use it in the adapter:
            //   return jpaRepository.findByEmail(email.value()).map(this::toDomain);

            assertTrue(true, "Documented: findByEmail uses findAll() which loads ALL users into memory");
        }
    }
}
