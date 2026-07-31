package com.aegis.identity.application.service;

import com.aegis.identity.domain.event.UserRegistered;
import com.aegis.identity.domain.exception.DuplicateEmailException;
import com.aegis.identity.domain.model.Email;
import com.aegis.identity.domain.model.User;
import com.aegis.identity.domain.port.inbound.RegisterUserUseCase;
import com.aegis.identity.domain.port.outbound.EventPublisher;
import com.aegis.identity.domain.port.outbound.PasswordHasher;
import com.aegis.identity.domain.port.outbound.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class RegisterUserService implements RegisterUserUseCase {

    private final UserRepository userRepository;
    private final EventPublisher eventPublisher;
    private final PasswordHasher passwordHasher;

    public RegisterUserService(UserRepository userRepository,
                               EventPublisher eventPublisher,
                               PasswordHasher passwordHasher) {
        this.userRepository = userRepository;
        this.eventPublisher = eventPublisher;
        this.passwordHasher = passwordHasher;
    }

    @Override
    @Transactional
    public Result register(Command command) {
        Email email = Email.of(command.email());

        if (userRepository.existsByEmail(email)) {
            throw new DuplicateEmailException(command.email());
        }

        User user = User.register(
                command.email(),
                command.password(),
                command.firstName(),
                command.lastName(),
                passwordHasher
        );

        User savedUser = userRepository.save(user);

        UserRegistered event = savedUser.toRegisteredEvent(command.correlationId());
        eventPublisher.publish(event);

        return new Result(
                savedUser.getUserId().value(),
                savedUser.getEmail().value(),
                savedUser.getStatus().name(),
                savedUser.getRegisteredAt()
        );
    }
}
