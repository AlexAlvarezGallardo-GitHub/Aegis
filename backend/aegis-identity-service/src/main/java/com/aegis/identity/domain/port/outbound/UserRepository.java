package com.aegis.identity.domain.port.outbound;

import com.aegis.identity.domain.model.Email;
import com.aegis.identity.domain.model.User;
import com.aegis.identity.domain.model.UserId;

import java.util.Optional;

public interface UserRepository {

    User save(User user);

    User saveAndFlush(User user);

    Optional<User> findByEmail(Email email);

    Optional<User> findById(UserId userId);

    boolean existsByEmail(Email email);
}
