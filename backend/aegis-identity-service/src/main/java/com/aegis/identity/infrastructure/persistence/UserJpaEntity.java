package com.aegis.identity.infrastructure.persistence;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Version;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "users")
public class UserJpaEntity {

    @Id
    private UUID id;

    @Column(nullable = false, unique = true, length = 255)
    private String email;

    @Column(name = "password_hash", nullable = false, length = 255)
    private String passwordHash;

    @Column(name = "first_name", nullable = false, length = 100)
    private String firstName;

    @Column(name = "last_name", nullable = false, length = 100)
    private String lastName;

    @Column(nullable = false, length = 30)
    private String status;

    @Column(name = "registered_at", nullable = false)
    private Instant registeredAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @Version
    private Long version;

    protected UserJpaEntity() {
    }

    public UserJpaEntity(UUID id, String email, String passwordHash, String firstName,
                          String lastName, String status, Instant registeredAt,
                          Instant updatedAt, Long version) {
        this.id = id;
        this.email = email;
        this.passwordHash = passwordHash;
        this.firstName = firstName;
        this.lastName = lastName;
        this.status = status;
        this.registeredAt = registeredAt;
        this.updatedAt = updatedAt;
        this.version = version;
    }

    public UUID getId() { return id; }
    public String getEmail() { return email; }
    public String getPasswordHash() { return passwordHash; }
    public String getFirstName() { return firstName; }
    public String getLastName() { return lastName; }
    public String getStatus() { return status; }
    public Instant getRegisteredAt() { return registeredAt; }
    public Instant getUpdatedAt() { return updatedAt; }
    public Long getVersion() { return version; }
}
