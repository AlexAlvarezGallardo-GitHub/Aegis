package com.aegis.identity.application.service;

import com.aegis.identity.domain.event.UserAccountLocked;
import com.aegis.identity.domain.event.UserAuthenticated;
import com.aegis.identity.domain.exception.AccountLockedException;
import com.aegis.identity.domain.exception.InvalidCredentialsException;
import com.aegis.identity.domain.model.Email;
import com.aegis.identity.domain.model.PasswordHash;
import com.aegis.identity.domain.model.TokenPair;
import com.aegis.identity.domain.model.User;
import com.aegis.identity.domain.model.UserId;
import com.aegis.identity.domain.model.UserStatus;
import com.aegis.identity.domain.port.inbound.AuthenticateUserUseCase;
import com.aegis.identity.domain.port.outbound.EventPublisher;
import com.aegis.identity.domain.port.outbound.PasswordHasher;
import com.aegis.identity.domain.port.outbound.TokenProvider;
import com.aegis.identity.domain.port.outbound.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AuthenticateUserServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private PasswordHasher passwordHasher;

    @Mock
    private TokenProvider tokenProvider;

    @Mock
    private EventPublisher eventPublisher;

    private AuthenticateUserService service;
    private User activeUser;

    @BeforeEach
    void setUp() {
        service = new AuthenticateUserService(userRepository, passwordHasher, tokenProvider, eventPublisher);

        PasswordHasher testHasher = new PasswordHasher() {
            @Override
            public PasswordHash hash(String rawPassword) {
                return PasswordHash.of("hashed_" + rawPassword);
            }

            @Override
            public boolean matches(String rawPassword, PasswordHash hash) {
                return ("hashed_" + rawPassword).equals(hash.hash());
            }
        };

        activeUser = User.register("user@example.com", "SecureP@ss1", "John", "Doe", testHasher);
    }

    @Test
    void shouldAuthenticateSuccessfully() {
        when(userRepository.findByEmail(any(Email.class))).thenReturn(Optional.of(activeUser));
        when(passwordHasher.matches(any(), any())).thenReturn(true);
        when(userRepository.saveAndFlush(any(User.class))).thenAnswer(inv -> inv.getArgument(0));
        when(tokenProvider.generateTokenPair(any(UserId.class), anyString()))
                .thenReturn(new TokenPair("access-token", "refresh-token"));

        AuthenticateUserUseCase.Command command = new AuthenticateUserUseCase.Command(
                "user@example.com", "SecureP@ss1", "corr-123");

        AuthenticateUserUseCase.Result result = service.authenticate(command);

        assertNotNull(result.tokenPair());
        assertEquals("access-token", result.tokenPair().accessToken());
        assertEquals("refresh-token", result.tokenPair().refreshToken());

        verify(userRepository).saveAndFlush(any(User.class));
        verify(eventPublisher).publish(any(UserAuthenticated.class));
        verify(tokenProvider).generateTokenPair(any(UserId.class), anyString());
    }

    @Test
    void shouldPublishFailedAuthenticationEvent() {
        when(userRepository.findByEmail(any(Email.class))).thenReturn(Optional.of(activeUser));
        when(passwordHasher.matches(any(), any())).thenReturn(false);
        when(userRepository.saveAndFlush(any(User.class))).thenAnswer(inv -> inv.getArgument(0));

        AuthenticateUserUseCase.Command command = new AuthenticateUserUseCase.Command(
                "user@example.com", "WrongP@ss1", "corr-123");

        assertThrows(InvalidCredentialsException.class, () -> service.authenticate(command));

        ArgumentCaptor<UserAuthenticated> captor = ArgumentCaptor.forClass(UserAuthenticated.class);
        verify(eventPublisher).publish(captor.capture());
        assertFalse(captor.getValue().success());
        assertEquals("INVALID_CREDENTIALS", captor.getValue().failureReason());
    }

    @Test
    void shouldThrowInvalidCredentialsForNonExistentUser() {
        when(userRepository.findByEmail(any(Email.class))).thenReturn(Optional.empty());

        AuthenticateUserUseCase.Command command = new AuthenticateUserUseCase.Command(
                "unknown@example.com", "password", "corr-123");

        assertThrows(InvalidCredentialsException.class, () -> service.authenticate(command));

        verify(eventPublisher, never()).publish(any(UserAuthenticated.class));
        verify(tokenProvider, never()).generateTokenPair(any(), any());
    }

    @Test
    void shouldPublishAccountLockedEventAfterThreshold() {
        when(userRepository.findByEmail(any(Email.class))).thenReturn(Optional.of(activeUser));
        when(passwordHasher.matches(any(), any())).thenReturn(false);
        when(userRepository.saveAndFlush(any(User.class))).thenAnswer(inv -> inv.getArgument(0));

        for (int i = 0; i < 5; i++) {
            AuthenticateUserUseCase.Command command = new AuthenticateUserUseCase.Command(
                    "user@example.com", "WrongP@ss1", "corr-" + i);
            assertThrows(InvalidCredentialsException.class, () -> service.authenticate(command));
        }

        verify(eventPublisher, times(5)).publish(any(UserAuthenticated.class));
        verify(eventPublisher).publish(any(UserAccountLocked.class));
    }

    @Test
    void shouldNotAuthenticateLockedAccount() {
        User lockedUser = User.rehydrate(
                activeUser.getUserId(), activeUser.getEmail(), activeUser.getPasswordHash(),
                activeUser.getFirstName(), activeUser.getLastName(), UserStatus.LOCKED,
                5, Instant.now().plusSeconds(300), Instant.now(), Instant.now(), 1L);

        when(userRepository.findByEmail(any(Email.class))).thenReturn(Optional.of(lockedUser));

        AuthenticateUserUseCase.Command command = new AuthenticateUserUseCase.Command(
                "user@example.com", "SecureP@ss1", "corr-123");

        assertThrows(AccountLockedException.class, () -> service.authenticate(command));
    }
}
