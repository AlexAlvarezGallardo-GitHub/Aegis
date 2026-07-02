package com.aegis.identity.domain.model;

import com.aegis.identity.domain.event.UserAccountLocked;
import com.aegis.identity.domain.event.UserAuthenticated;
import com.aegis.identity.domain.event.UserRegistered;
import com.aegis.identity.domain.exception.AccountLockedException;
import com.aegis.identity.domain.exception.AccountSuspendedException;
import com.aegis.identity.domain.port.outbound.PasswordHasher;

import java.time.Instant;
import java.util.Objects;

public class User {

    private static final int MAX_FAILED_ATTEMPTS = 5;

    private final UserId userId;
    private final Email email;
    private final PasswordHash passwordHash;
    private final String firstName;
    private final String lastName;
    private UserStatus status;
    private final Instant registeredAt;
    private int failedLoginAttempts;
    private Instant lockedUntil;
    private Instant updatedAt;
    private long version;

    private User(UserId userId, Email email, PasswordHash passwordHash,
                 String firstName, String lastName) {
        this.userId = Objects.requireNonNull(userId, "UserId must not be null");
        this.email = Objects.requireNonNull(email, "Email must not be null");
        this.passwordHash = Objects.requireNonNull(passwordHash, "PasswordHash must not be null");
        this.firstName = Objects.requireNonNull(firstName, "First name must not be null");
        this.lastName = Objects.requireNonNull(lastName, "Last name must not be null");
        this.status = UserStatus.PENDING_VERIFICATION;
        this.registeredAt = Instant.now();
        this.failedLoginAttempts = 0;
        this.lockedUntil = null;
        this.updatedAt = this.registeredAt;
        this.version = 0;
    }

    private User(UserId userId, Email email, PasswordHash passwordHash,
                 String firstName, String lastName, UserStatus status,
                 int failedLoginAttempts, Instant lockedUntil,
                 Instant registeredAt, Instant updatedAt, long version) {
        this.userId = userId;
        this.email = email;
        this.passwordHash = passwordHash;
        this.firstName = firstName;
        this.lastName = lastName;
        this.status = status;
        this.failedLoginAttempts = failedLoginAttempts;
        this.lockedUntil = lockedUntil;
        this.registeredAt = registeredAt;
        this.updatedAt = updatedAt;
        this.version = version;
    }

    public static User register(String rawEmail, String rawPassword,
                                String firstName, String lastName,
                                PasswordHasher hasher) {
        validateName(firstName, "First name");
        validateName(lastName, "Last name");

        UserId userId = UserId.generate();
        Email email = Email.of(rawEmail);
        PasswordHash passwordHash = hasher.hash(rawPassword);

        return new User(userId, email, passwordHash, firstName.trim(), lastName.trim());
    }

    public static User rehydrate(UserId userId, Email email, PasswordHash passwordHash,
                                  String firstName, String lastName, UserStatus status,
                                  int failedLoginAttempts, Instant lockedUntil,
                                  Instant registeredAt, Instant updatedAt, long version) {
        return new User(userId, email, passwordHash, firstName, lastName,
                status, failedLoginAttempts, lockedUntil, registeredAt, updatedAt, version);
    }

    public UserRegistered toRegisteredEvent(String correlationId) {
        return new UserRegistered(
                userId.value(),
                email.value(),
                firstName,
                lastName,
                registeredAt,
                correlationId
        );
    }

    public UserAuthenticated authenticate(String rawPassword, PasswordHasher hasher, String correlationId) {
        if (status == UserStatus.LOCKED) {
            if (lockedUntil != null && Instant.now().isBefore(lockedUntil)) {
                throw new AccountLockedException();
            }
            status = UserStatus.ACTIVE;
            failedLoginAttempts = 0;
            lockedUntil = null;
        }

        if (status == UserStatus.SUSPENDED) {
            throw new AccountSuspendedException();
        }

        if (!hasher.matches(rawPassword, passwordHash)) {
            failedLoginAttempts++;
            updatedAt = Instant.now();

            if (failedLoginAttempts >= MAX_FAILED_ATTEMPTS) {
                status = UserStatus.LOCKED;
                lockedUntil = Instant.now().plusSeconds(900);
            }

            return new UserAuthenticated(
                    userId.value(),
                    email.value(),
                    false,
                    "INVALID_CREDENTIALS",
                    correlationId
            );
        }

        failedLoginAttempts = 0;
        lockedUntil = null;
        updatedAt = Instant.now();

        return new UserAuthenticated(
                userId.value(),
                email.value(),
                true,
                null,
                correlationId
        );
    }

    public UserAccountLocked toAccountLockedEvent(String correlationId) {
        if (status != UserStatus.LOCKED) {
            throw new IllegalStateException("Account is not locked");
        }
        return new UserAccountLocked(
                userId.value(),
                email.value(),
                failedLoginAttempts,
                correlationId
        );
    }

    public boolean isLockedDueToFailures() {
        return status == UserStatus.LOCKED && failedLoginAttempts >= MAX_FAILED_ATTEMPTS;
    }

    private static void validateName(String name, String fieldName) {
        if (name == null || name.trim().isEmpty()) {
            throw new IllegalArgumentException(fieldName + " must not be blank");
        }
        if (name.trim().length() > 100) {
            throw new IllegalArgumentException(fieldName + " must not exceed 100 characters");
        }
    }

    public UserId getUserId() { return userId; }
    public Email getEmail() { return email; }
    public PasswordHash getPasswordHash() { return passwordHash; }
    public String getFirstName() { return firstName; }
    public String getLastName() { return lastName; }
    public UserStatus getStatus() { return status; }
    public int getFailedLoginAttempts() { return failedLoginAttempts; }
    public Instant getLockedUntil() { return lockedUntil; }
    public Instant getRegisteredAt() { return registeredAt; }
    public Instant getUpdatedAt() { return updatedAt; }
    public long getVersion() { return version; }
}
