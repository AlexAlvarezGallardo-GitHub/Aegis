package com.aegis.identity.domain.model;

import com.aegis.identity.domain.event.UserRegistered;
import com.aegis.identity.domain.port.outbound.PasswordHasher;

import java.time.Instant;
import java.util.Objects;

public class User {

    private final UserId userId;
    private final Email email;
    private final PasswordHash passwordHash;
    private final String firstName;
    private final String lastName;
    private UserStatus status;
    private final Instant registeredAt;
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
        this.updatedAt = this.registeredAt;
        this.version = 0;
    }

    private User(UserId userId, Email email, PasswordHash passwordHash,
                 String firstName, String lastName, UserStatus status,
                 Instant registeredAt, Instant updatedAt, long version) {
        this.userId = userId;
        this.email = email;
        this.passwordHash = passwordHash;
        this.firstName = firstName;
        this.lastName = lastName;
        this.status = status;
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
                                  Instant registeredAt, Instant updatedAt, long version) {
        return new User(userId, email, passwordHash, firstName, lastName,
                status, registeredAt, updatedAt, version);
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
    public Instant getRegisteredAt() { return registeredAt; }
    public Instant getUpdatedAt() { return updatedAt; }
    public long getVersion() { return version; }
}
