package com.aegis.identity.application.service;

import com.aegis.identity.application.dto.AuthenticateUserCommand;
import com.aegis.identity.application.dto.AuthenticationResponse;
import com.aegis.identity.application.mapper.AuthMapper;
import com.aegis.identity.domain.event.UserAccountLocked;
import com.aegis.identity.domain.event.UserAuthenticated;
import com.aegis.identity.domain.exception.InvalidCredentialsException;
import com.aegis.identity.domain.model.Email;
import com.aegis.identity.domain.model.TokenPair;
import com.aegis.identity.domain.model.User;
import com.aegis.identity.domain.model.UserStatus;
import com.aegis.identity.domain.port.inbound.AuthenticateUserUseCase;
import com.aegis.identity.domain.port.outbound.EventPublisher;
import com.aegis.identity.domain.port.outbound.PasswordHasher;
import com.aegis.identity.domain.port.outbound.TokenProvider;
import com.aegis.identity.domain.port.outbound.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AuthenticateUserService implements AuthenticateUserUseCase {

    private final UserRepository userRepository;
    private final PasswordHasher passwordHasher;
    private final TokenProvider tokenProvider;
    private final EventPublisher eventPublisher;

    public AuthenticateUserService(UserRepository userRepository,
                                   PasswordHasher passwordHasher,
                                   TokenProvider tokenProvider,
                                   EventPublisher eventPublisher) {
        this.userRepository = userRepository;
        this.passwordHasher = passwordHasher;
        this.tokenProvider = tokenProvider;
        this.eventPublisher = eventPublisher;
    }

    @Override
    @Transactional
    public Result authenticate(Command command) {
        Email email = Email.of(command.email());

        User user = userRepository.findByEmail(email)
                .orElseThrow(InvalidCredentialsException::new);

        UserAuthenticated authEvent = user.authenticate(
                command.password(),
                passwordHasher,
                command.correlationId()
        );

        userRepository.saveAndFlush(user);

        eventPublisher.publish(authEvent);

        if (!authEvent.success()) {
            if (user.isLockedDueToFailures()) {
                UserAccountLocked lockedEvent = user.toAccountLockedEvent(command.correlationId());
                eventPublisher.publish(lockedEvent);
            }
            throw new InvalidCredentialsException();
        }

        TokenPair tokenPair = tokenProvider.generateTokenPair(
                user.getUserId(),
                user.getEmail().value()
        );

        boolean emailVerified = user.getStatus() != UserStatus.PENDING_VERIFICATION;
        return new Result(tokenPair, emailVerified);
    }

    public AuthenticationResponse authenticateAndReturnResponse(AuthenticateUserCommand command) {
        Command useCaseCommand = new Command(
                command.email(),
                command.password(),
                command.correlationId()
        );

        Result result = authenticate(useCaseCommand);

        return AuthMapper.toResponse(result.tokenPair(), result.emailVerified());
    }
}
