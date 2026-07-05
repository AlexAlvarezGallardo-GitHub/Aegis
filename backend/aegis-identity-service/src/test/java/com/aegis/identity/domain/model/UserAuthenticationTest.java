package com.aegis.identity.domain.model;

import com.aegis.identity.domain.event.UserAccountLocked;
import com.aegis.identity.domain.event.UserAuthenticated;
import com.aegis.identity.domain.exception.AccountLockedException;
import com.aegis.identity.domain.exception.AccountSuspendedException;
import com.aegis.identity.domain.port.outbound.PasswordHasher;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Instant;

import static org.junit.jupiter.api.Assertions.*;

class UserAuthenticationTest {

    private PasswordHasher passwordHasher;
    private User activeUser;

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

        activeUser = User.register("user@example.com", "SecureP@ss1", "John", "Doe", passwordHasher);
    }

    @Test
    void shouldAuthenticateWithValidCredentials() {
        UserAuthenticated event = activeUser.authenticate("SecureP@ss1", passwordHasher, "corr-123");

        assertTrue(event.success());
        assertNull(event.failureReason());
        assertEquals("USER_AUTHENTICATED", event.eventType());
        assertEquals("1.0", event.schemaVersion());
        assertEquals("corr-123", event.correlationId());
        assertEquals(0, activeUser.getFailedLoginAttempts());
    }

    @Test
    void shouldFailAuthenticationWithWrongPassword() {
        UserAuthenticated event = activeUser.authenticate("WrongP@ss1", passwordHasher, "corr-123");

        assertFalse(event.success());
        assertEquals("INVALID_CREDENTIALS", event.failureReason());
        assertEquals(1, activeUser.getFailedLoginAttempts());
    }

    @Test
    void shouldLockAccountAfterFiveFailedAttempts() {
        for (int i = 0; i < 5; i++) {
            activeUser.authenticate("WrongP@ss1", passwordHasher, "corr-" + i);
        }

        assertEquals(5, activeUser.getFailedLoginAttempts());
        assertEquals(UserStatus.LOCKED, activeUser.getStatus());
        assertTrue(activeUser.isLockedDueToFailures());
        assertNotNull(activeUser.getLockedUntil());
    }

    @Test
    void shouldResetFailedAttemptsOnSuccessfulLogin() {
        for (int i = 0; i < 3; i++) {
            activeUser.authenticate("WrongP@ss1", passwordHasher, "corr-" + i);
        }
        assertEquals(3, activeUser.getFailedLoginAttempts());

        UserAuthenticated event = activeUser.authenticate("SecureP@ss1", passwordHasher, "corr-success");
        assertTrue(event.success());
        assertEquals(0, activeUser.getFailedLoginAttempts());
        assertNull(activeUser.getLockedUntil());
    }

    @Test
    void shouldRejectAuthenticationWhenSuspended() {
        User suspendedUser = User.rehydrate(
                activeUser.getUserId(), activeUser.getEmail(), activeUser.getPasswordHash(),
                activeUser.getFirstName(), activeUser.getLastName(), UserStatus.SUSPENDED,
                0, null, activeUser.getRegisteredAt(), Instant.now(), 1L);

        assertThrows(AccountSuspendedException.class,
                () -> suspendedUser.authenticate("SecureP@ss1", passwordHasher, "corr-123"));
    }

    @Test
    void shouldRejectAuthenticationWhenLocked() {
        User lockedUser = User.rehydrate(
                activeUser.getUserId(), activeUser.getEmail(), activeUser.getPasswordHash(),
                activeUser.getFirstName(), activeUser.getLastName(), UserStatus.LOCKED,
                5, Instant.now().plusSeconds(300), activeUser.getRegisteredAt(), Instant.now(), 1L);

        assertThrows(AccountLockedException.class,
                () -> lockedUser.authenticate("SecureP@ss1", passwordHasher, "corr-123"));
    }

    @Test
    void shouldGenerateAccountLockedEvent() {
        for (int i = 0; i < 5; i++) {
            activeUser.authenticate("WrongP@ss1", passwordHasher, "corr-" + i);
        }

        UserAccountLocked event = activeUser.toAccountLockedEvent("corr-lock");
        assertEquals(activeUser.getUserId().value(), event.userId());
        assertEquals("USER_ACCOUNT_LOCKED", event.eventType());
        assertEquals(5, event.failureCount());
        assertEquals("corr-lock", event.correlationId());
    }

    @Test
    void shouldResetFailedAttemptsOnRehydrate() {
        User user = User.rehydrate(
                UserId.generate(), Email.of("test@example.com"),
                PasswordHash.of("hash"), "Test", "User",
                UserStatus.ACTIVE, 0, null,
                Instant.now(), Instant.now(), 1L);

        assertEquals(0, user.getFailedLoginAttempts());
        assertNull(user.getLockedUntil());
    }
}
