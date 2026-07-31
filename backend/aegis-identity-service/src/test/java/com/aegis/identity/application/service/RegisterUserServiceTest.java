package com.aegis.identity.application.service;

import com.aegis.identity.domain.event.UserRegistered;
import com.aegis.identity.domain.exception.DuplicateEmailException;
import com.aegis.identity.domain.exception.InvalidEmailException;
import com.aegis.identity.domain.exception.WeakPasswordException;
import com.aegis.identity.domain.model.Email;
import com.aegis.identity.domain.model.PasswordHash;
import com.aegis.identity.domain.model.User;
import com.aegis.identity.domain.port.inbound.RegisterUserUseCase;
import com.aegis.identity.domain.port.outbound.EventPublisher;
import com.aegis.identity.domain.port.outbound.PasswordHasher;
import com.aegis.identity.domain.port.outbound.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class RegisterUserServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private EventPublisher eventPublisher;

    @Mock
    private PasswordHasher passwordHasher;

    private RegisterUserService service;

    @BeforeEach
    void setUp() {
        service = new RegisterUserService(userRepository, eventPublisher, passwordHasher);
    }

    @Test
    void shouldRegisterUserSuccessfully() {
        when(userRepository.existsByEmail(any(Email.class))).thenReturn(false);
        when(passwordHasher.hash(any())).thenReturn(PasswordHash.of("hashed"));
        when(userRepository.save(any(User.class))).thenAnswer(inv -> inv.getArgument(0));

        RegisterUserUseCase.Command command = new RegisterUserUseCase.Command(
                "user@example.com", "SecureP@ss1", "John", "Doe", "corr-123");

        RegisterUserUseCase.Result result = service.register(command);

        assertNotNull(result.userId());
        assertEquals("user@example.com", result.email());
        assertEquals("PENDING_VERIFICATION", result.status());
        assertNotNull(result.registeredAt());

        verify(userRepository).save(any(User.class));
        verify(eventPublisher).publish(any(UserRegistered.class));
    }

    @Test
    void shouldPublishUserRegisteredEvent() {
        when(userRepository.existsByEmail(any(Email.class))).thenReturn(false);
        when(passwordHasher.hash(any())).thenReturn(PasswordHash.of("hashed"));
        when(userRepository.save(any(User.class))).thenAnswer(inv -> inv.getArgument(0));

        RegisterUserUseCase.Command command = new RegisterUserUseCase.Command(
                "user@example.com", "SecureP@ss1", "John", "Doe", "corr-123");

        service.register(command);

        ArgumentCaptor<UserRegistered> captor = ArgumentCaptor.forClass(UserRegistered.class);
        verify(eventPublisher).publish(captor.capture());

        UserRegistered event = captor.getValue();
        assertEquals("user@example.com", event.email());
        assertEquals("John", event.firstName());
        assertEquals("Doe", event.lastName());
        assertEquals("corr-123", event.correlationId());
    }

    @Test
    void shouldRejectDuplicateEmail() {
        when(userRepository.existsByEmail(any(Email.class))).thenReturn(true);

        RegisterUserUseCase.Command command = new RegisterUserUseCase.Command(
                "user@example.com", "SecureP@ss1", "John", "Doe", "corr-123");

        assertThrows(DuplicateEmailException.class, () -> service.register(command));

        verify(userRepository, never()).save(any());
        verify(eventPublisher, never()).publish(any(UserRegistered.class));
    }

    @Test
    void shouldRejectInvalidEmail() {
        RegisterUserUseCase.Command command = new RegisterUserUseCase.Command(
                "not-an-email", "SecureP@ss1", "John", "Doe", "corr-123");

        assertThrows(InvalidEmailException.class, () -> service.register(command));

        verify(userRepository, never()).save(any());
    }

    @Test
    void shouldRejectWeakPassword() {
        when(userRepository.existsByEmail(any(Email.class))).thenReturn(false);

        RegisterUserUseCase.Command command = new RegisterUserUseCase.Command(
                "user@example.com", "weak", "John", "Doe", "corr-123");

        assertThrows(WeakPasswordException.class, () -> service.register(command));

        verify(userRepository, never()).save(any());
        verify(eventPublisher, never()).publish(any(UserRegistered.class));
    }
}
