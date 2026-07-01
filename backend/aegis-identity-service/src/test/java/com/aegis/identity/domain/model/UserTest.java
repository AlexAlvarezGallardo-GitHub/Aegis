package com.aegis.identity.domain.model;

import com.aegis.identity.domain.event.UserRegistered;
import com.aegis.identity.domain.port.outbound.PasswordHasher;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class UserTest {

    private PasswordHasher passwordHasher;

    @BeforeEach
    void setUp() {
        passwordHasher = new PasswordHasher() {
            @Override
            public PasswordHash hash(String rawPassword) {
                return PasswordHash.of("hashed_" + rawPassword);
            }

            @Override
            public boolean matches(String rawPassword, PasswordHash hash) {
                return ("hashed_" + rawPassword).equals(hash.hash());
            }
        };
    }

    @Test
    void shouldRegisterUserWithValidData() {
        User user = User.register("user@example.com", "SecureP@ss1", "John", "Doe", passwordHasher);

        assertNotNull(user.getUserId());
        assertEquals("user@example.com", user.getEmail().value());
        assertEquals("John", user.getFirstName());
        assertEquals("Doe", user.getLastName());
        assertEquals(UserStatus.PENDING_VERIFICATION, user.getStatus());
        assertNotNull(user.getRegisteredAt());
        assertEquals(0, user.getVersion());
    }

    @Test
    void shouldTrimNames() {
        User user = User.register("user@example.com", "SecureP@ss1", "  John  ", "  Doe  ", passwordHasher);

        assertEquals("John", user.getFirstName());
        assertEquals("Doe", user.getLastName());
    }

    @Test
    void shouldRejectBlankFirstName() {
        assertThrows(IllegalArgumentException.class,
                () -> User.register("user@example.com", "SecureP@ss1", "", "Doe", passwordHasher));
    }

    @Test
    void shouldRejectBlankLastName() {
        assertThrows(IllegalArgumentException.class,
                () -> User.register("user@example.com", "SecureP@ss1", "John", "", passwordHasher));
    }

    @Test
    void shouldRejectFirstNameExceedingMaxLength() {
        String longName = "a".repeat(101);
        assertThrows(IllegalArgumentException.class,
                () -> User.register("user@example.com", "SecureP@ss1", longName, "Doe", passwordHasher));
    }

    @Test
    void shouldRejectInvalidEmail() {
        assertThrows(Exception.class,
                () -> User.register("not-an-email", "SecureP@ss1", "John", "Doe", passwordHasher));
    }

    @Test
    void shouldGenerateRegisteredEvent() {
        User user = User.register("user@example.com", "SecureP@ss1", "John", "Doe", passwordHasher);

        UserRegistered event = user.toRegisteredEvent("corr-123");

        assertEquals(user.getUserId().value(), event.userId());
        assertEquals("user@example.com", event.email());
        assertEquals("John", event.firstName());
        assertEquals("Doe", event.lastName());
        assertEquals("USER_REGISTERED", event.eventType());
        assertEquals("1.0", event.schemaVersion());
        assertEquals("corr-123", event.correlationId());
        assertNotNull(event.eventId());
    }

    @Test
    void shouldRehydrateUser() {
        UserId userId = UserId.generate();
        Email email = Email.of("user@example.com");
        PasswordHash hash = PasswordHash.of("hash123");

        User user = User.rehydrate(userId, email, hash, "John", "Doe",
                UserStatus.ACTIVE, java.time.Instant.now(), java.time.Instant.now(), 5L);

        assertEquals(userId, user.getUserId());
        assertEquals(UserStatus.ACTIVE, user.getStatus());
        assertEquals(5L, user.getVersion());
    }
}
