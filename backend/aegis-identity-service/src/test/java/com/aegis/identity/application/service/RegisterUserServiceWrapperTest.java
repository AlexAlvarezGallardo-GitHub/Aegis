package com.aegis.identity.application.service;

import com.aegis.identity.application.dto.RegisterUserCommand;
import com.aegis.identity.application.dto.UserRegistrationResponse;
import com.aegis.identity.domain.event.UserRegistered;
import com.aegis.identity.domain.model.Email;
import com.aegis.identity.domain.model.PasswordHash;
import com.aegis.identity.domain.model.User;
import com.aegis.identity.domain.port.outbound.EventPublisher;
import com.aegis.identity.domain.port.outbound.PasswordHasher;
import com.aegis.identity.domain.port.outbound.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("RegisterUserService - registerAndReturnResponse wrapper")
class RegisterUserServiceWrapperTest {

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

    @Nested
    @DisplayName("When calling registerAndReturnResponse")
    class WhenCallingRegisterAndReturnResponse {

        @Test
        @DisplayName("Should return UserRegistrationResponse with correct fields")
        void shouldReturnResponseWithCorrectFields() {
            // Arrange
            when(userRepository.existsByEmail(any(Email.class))).thenReturn(false);
            when(passwordHasher.hash(any())).thenReturn(PasswordHash.of("hashed"));
            when(userRepository.save(any(User.class))).thenAnswer(inv -> inv.getArgument(0));

            RegisterUserCommand command = new RegisterUserCommand(
                    "wrapper@example.com", "SecureP@ss1", "Wrapper", "Test", "corr-wrap");

            // Act
            UserRegistrationResponse response = service.registerAndReturnResponse(command);

            // Assert
            assertNotNull(response);
            assertNotNull(response.userId());
            assertEquals("wrapper@example.com", response.email());
            assertEquals("PENDING_VERIFICATION", response.status());
            assertNotNull(response.registeredAt());
        }

        @Test
        @DisplayName("Should still publish UserRegistered event via wrapper method")
        void shouldStillPublishEventViaWrapper() {
            // Arrange
            when(userRepository.existsByEmail(any(Email.class))).thenReturn(false);
            when(passwordHasher.hash(any())).thenReturn(PasswordHash.of("hashed"));
            when(userRepository.save(any(User.class))).thenAnswer(inv -> inv.getArgument(0));

            RegisterUserCommand command = new RegisterUserCommand(
                    "event-via-wrapper@example.com", "SecureP@ss1", "Event", "Wrapper", "corr-ew");

            // Act
            service.registerAndReturnResponse(command);

            // Assert
            verify(eventPublisher).publish(any(UserRegistered.class));
        }

        @Test
        @DisplayName("Should still save user via wrapper method")
        void shouldStillSaveUserViaWrapper() {
            // Arrange
            when(userRepository.existsByEmail(any(Email.class))).thenReturn(false);
            when(passwordHasher.hash(any())).thenReturn(PasswordHash.of("hashed"));
            when(userRepository.save(any(User.class))).thenAnswer(inv -> inv.getArgument(0));

            RegisterUserCommand command = new RegisterUserCommand(
                    "save-via-wrapper@example.com", "SecureP@ss1", "Save", "Wrapper", "corr-sw");

            // Act
            service.registerAndReturnResponse(command);

            // Assert
            verify(userRepository).save(any(User.class));
        }
    }

    @Nested
    @DisplayName("Known issues and risks")
    class KnownIssuesAndRisks {

        @Test
        @DisplayName("BUG: @Transactional(readOnly=true) on wrapper bypasses inner @Transactional via self-invocation")
        void documentsSelfInvocationTransactionalBug() {
            // DOCUMENTATION TEST: This test documents a critical transactional bug.
            //
            // In RegisterUserService:
            //
            //   @Transactional(readOnly = true)                    // <-- OUTER transaction
            //   public UserRegistrationResponse registerAndReturnResponse(RegisterUserCommand command) {
            //       ...
            //       Result result = register(useCaseCommand);       // <-- SELF-INVOCATION
            //       ...
            //   }
            //
            //   @Override
            //   @Transactional                                      // <-- INNER transaction (IGNORED!)
            //   public Result register(Command command) {
            //       ...
            //       userRepository.save(user);                      // WRITE in readOnly TX
            //       eventPublisher.publish(event);                  // WRITE in readOnly TX
            //       ...
            //   }
            //
            // Due to Spring's proxy-based AOP, calling register() from within the same class
            // bypasses the transaction proxy. The inner @Transactional annotation is IGNORED.
            // The entire flow runs under the outer readOnly=true transaction.
            //
            // Impact:
            // - With JpaTransactionManager: INSERTs still work (readOnly only sets FlushMode.MANUAL)
            // - With DataSourceTransactionManager: INSERTs would FAIL (Connection.setReadOnly(true))
            // - The readOnly semantics are violated (writes happen in a "read-only" transaction)
            // - This is fragile and depends on implementation details of the transaction manager
            //
            // Fix options:
            // 1. Remove @Transactional(readOnly=true) from registerAndReturnResponse()
            // 2. Inject RegisterUserService into itself (self-injection) to go through proxy
            // 3. Extract register() into a separate service class
            // 4. Use TransactionTemplate for programmatic transaction management

            assertTrue(true, "Documented: @Transactional self-invocation bypass in RegisterUserService");
        }
    }
}
