package com.aegis.identity.web.controller;

import com.aegis.identity.infrastructure.persistence.OutboxEventJpaEntity;
import com.aegis.identity.infrastructure.persistence.OutboxEventJpaRepository;
import com.aegis.identity.infrastructure.persistence.UserJpaEntity;
import com.aegis.identity.infrastructure.persistence.UserJpaRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.testcontainers.containers.KafkaContainer;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

import java.util.List;
import java.util.Map;

import static org.hamcrest.Matchers.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@Testcontainers
class RegistrationControllerIT {

    @Container
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16-alpine")
            .withDatabaseName("test_identity")
            .withUsername("test")
            .withPassword("test");

    @Container
    static KafkaContainer kafka = new KafkaContainer(
            DockerImageName.parse("confluentinc/cp-kafka:7.5.0"));

    @DynamicPropertySource
    static void configureProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
        registry.add("spring.kafka.bootstrap-servers", kafka::getBootstrapServers);
        registry.add("spring.flyway.enabled", () -> "true");
        registry.add("aegis.jwt.secret", () -> "test-secret-that-is-at-least-256-bits-long-for-hs256-algorithm");
    }

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private UserJpaRepository userJpaRepository;

    @Autowired
    private OutboxEventJpaRepository outboxEventJpaRepository;

    @BeforeEach
    void setUp() {
        outboxEventJpaRepository.deleteAll();
        userJpaRepository.deleteAll();
    }

    @Test
    void shouldRegisterUserSuccessfully() throws Exception {
        Map<String, String> request = Map.of(
                "email", "john@example.com",
                "password", "SecureP@ss1",
                "firstName", "John",
                "lastName", "Doe"
        );

        mockMvc.perform(post("/api/v1/users/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.userId").isNotEmpty())
                .andExpect(jsonPath("$.email").value("john@example.com"))
                .andExpect(jsonPath("$.status").value("PENDING_VERIFICATION"))
                .andExpect(jsonPath("$.registeredAt").isNotEmpty());

        List<UserJpaEntity> users = userJpaRepository.findAll();
        assertEquals(1, users.size());
        assertEquals("john@example.com", users.get(0).getEmail());
        assertEquals("PENDING_VERIFICATION", users.get(0).getStatus());
        assertNotEquals("SecureP@ss1", users.get(0).getPasswordHash());

        List<OutboxEventJpaEntity> events = outboxEventJpaRepository.findAll();
        assertEquals(1, events.size());
        assertEquals("USER_REGISTERED", events.get(0).getEventType());
        assertEquals("PENDING", events.get(0).getStatus());
    }

    @Test
    void shouldRejectDuplicateEmail() throws Exception {
        Map<String, String> request = Map.of(
                "email", "john@example.com",
                "password", "SecureP@ss1",
                "firstName", "John",
                "lastName", "Doe"
        );

        mockMvc.perform(post("/api/v1/users/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated());

        mockMvc.perform(post("/api/v1/users/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("EMAIL_ALREADY_REGISTERED"));
    }

    @Test
    void shouldRejectInvalidEmail() throws Exception {
        Map<String, String> request = Map.of(
                "email", "not-an-email",
                "password", "SecureP@ss1",
                "firstName", "John",
                "lastName", "Doe"
        );

        mockMvc.perform(post("/api/v1/users/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void shouldRejectWeakPassword() throws Exception {
        Map<String, String> request = Map.of(
                "email", "john@example.com",
                "password", "weak",
                "firstName", "John",
                "lastName", "Doe"
        );

        mockMvc.perform(post("/api/v1/users/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void shouldRejectMissingRequiredFields() throws Exception {
        Map<String, String> request = Map.of(
                "email", "john@example.com"
        );

        mockMvc.perform(post("/api/v1/users/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void shouldNotExposePasswordInResponse() throws Exception {
        Map<String, String> request = Map.of(
                "email", "john@example.com",
                "password", "SecureP@ss1",
                "firstName", "John",
                "lastName", "Doe"
        );

        mockMvc.perform(post("/api/v1/users/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.passwordHash").doesNotExist())
                .andExpect(jsonPath("$.password").doesNotExist());
    }
}
