package com.aegis.identity.infrastructure.persistence;

import com.aegis.identity.domain.model.Email;
import com.aegis.identity.domain.model.PasswordHash;
import com.aegis.identity.domain.model.User;
import com.aegis.identity.domain.model.UserId;
import com.aegis.identity.domain.model.UserStatus;
import com.aegis.identity.domain.port.outbound.UserRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public class UserRepositoryAdapter implements UserRepository {

    private final UserJpaRepository jpaRepository;

    public UserRepositoryAdapter(UserJpaRepository jpaRepository) {
        this.jpaRepository = jpaRepository;
    }

    @Override
    public User save(User user) {
        UserJpaEntity entity = toEntity(user);
        UserJpaEntity saved = jpaRepository.save(entity);
        return toDomain(saved);
    }

    @Override
    public User saveAndFlush(User user) {
        UserJpaEntity entity = toEntity(user);
        UserJpaEntity saved = jpaRepository.saveAndFlush(entity);
        return toDomain(saved);
    }

    @Override
    public Optional<User> findByEmail(Email email) {
        return jpaRepository.findByEmail(email.value())
                .map(this::toDomain);
    }

    @Override
    public Optional<User> findById(UserId userId) {
        return jpaRepository.findById(userId.value())
                .map(this::toDomain);
    }

    @Override
    public boolean existsByEmail(Email email) {
        return jpaRepository.existsByEmail(email.value());
    }

    private UserJpaEntity toEntity(User user) {
        return new UserJpaEntity(
                user.getUserId().value(),
                user.getEmail().value(),
                user.getPasswordHash().hash(),
                user.getFirstName(),
                user.getLastName(),
                user.getStatus().name(),
                user.getFailedLoginAttempts(),
                user.getLockedUntil(),
                user.getRegisteredAt(),
                user.getUpdatedAt(),
                user.getVersion()
        );
    }

    private User toDomain(UserJpaEntity entity) {
        return User.rehydrate(
                UserId.of(entity.getId()),
                Email.of(entity.getEmail()),
                PasswordHash.of(entity.getPasswordHash()),
                entity.getFirstName(),
                entity.getLastName(),
                UserStatus.valueOf(entity.getStatus()),
                entity.getFailedLoginAttempts(),
                entity.getLockedUntil(),
                entity.getRegisteredAt(),
                entity.getUpdatedAt(),
                entity.getVersion()
        );
    }
}
