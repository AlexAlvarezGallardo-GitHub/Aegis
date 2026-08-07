package com.aegis.wallet.infrastructure.messaging;

import io.micrometer.tracing.Span;
import io.micrometer.tracing.Tracer;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.KafkaContainer;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

import java.time.Duration;
import java.util.Collections;
import java.util.Properties;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Verifies that the Kafka producer propagates the W3C {@code traceparent} header
 * so a distributed trace spans the producer → Kafka → consumer chain (OTLP/Tempo).
 */
@SpringBootTest
@Testcontainers
@DisplayName("Kafka trace propagation")
class KafkaTracePropagationIT {

    private static final String TOPIC = "wallet.trace.propagation.test";

    @Container
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16-alpine")
            .withDatabaseName("test_trace")
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
    }

    @Autowired
    private KafkaTemplate<String, String> kafkaTemplate;

    @Autowired
    private Tracer tracer;

    @Test
    @DisplayName("Published message carries the W3C traceparent header when a span is active")
    void publishedMessageCarriesTraceparent() throws Exception {
        String key = UUID.randomUUID().toString();

        // Publish inside an active span so the producer attaches trace context
        Span span = tracer.nextSpan().name("test-publish").start();
        try (var ignored = tracer.withSpan(span)) {
            kafkaTemplate.send(TOPIC, key, "{\"event\":\"trace-test\"}").get();
        } finally {
            span.end();
        }

        // Consume and assert the traceparent header is present
        try (KafkaConsumer<String, String> consumer = new KafkaConsumer<>(consumerProps())) {
            consumer.subscribe(Collections.singletonList(TOPIC));
            long deadline = System.currentTimeMillis() + 15_000;
            String traceparent = null;
            while (System.currentTimeMillis() < deadline && traceparent == null) {
                ConsumerRecords<String, String> records = consumer.poll(Duration.ofMillis(1000));
                for (var record : records) {
                    var headers = record.headers().headers("traceparent");
                    if (headers.iterator().hasNext()) {
                        traceparent = new String(headers.iterator().next().value());
                    }
                }
            }

            assertNotNull(traceparent, "message should carry a W3C traceparent header");
            // format: <version>-<traceid>-<spanid>-<flags>, 55 chars
            assertEquals(55, traceparent.length());
            assertTrue(traceparent.startsWith("00-"), "traceparent should use W3C v00 format");
        }
    }

    private Properties consumerProps() {
        Properties props = new Properties();
        props.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, kafka.getBootstrapServers());
        props.put(ConsumerConfig.GROUP_ID_CONFIG, "trace-verify-" + UUID.randomUUID());
        props.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class.getName());
        props.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class.getName());
        props.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest");
        return props;
    }
}
