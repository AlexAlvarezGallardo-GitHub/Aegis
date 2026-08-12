package com.aegis.audit.infrastructure.config;

import com.aegis.audit.domain.event.FraudAssessmentCompletedEvent;
import com.aegis.audit.domain.event.FundsDepositedEvent;
import com.aegis.audit.domain.event.PaymentExecutedEvent;
import com.aegis.audit.domain.event.PaymentFailedEvent;
import com.aegis.audit.domain.event.PaymentRequestedEvent;
import com.aegis.audit.domain.event.TransferCompletedEvent;
import com.aegis.audit.domain.event.TransferFailedEvent;
import com.aegis.audit.domain.event.TransferRequestedEvent;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.apache.kafka.common.serialization.StringSerializer;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.kafka.KafkaProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.annotation.EnableKafka;
import org.springframework.kafka.config.ConcurrentKafkaListenerContainerFactory;
import org.springframework.kafka.core.ConsumerFactory;
import org.springframework.kafka.core.DefaultKafkaConsumerFactory;
import org.springframework.kafka.core.DefaultKafkaProducerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.core.ProducerFactory;
import org.springframework.kafka.listener.DeadLetterPublishingRecoverer;
import org.springframework.kafka.listener.DefaultErrorHandler;
import org.springframework.kafka.support.serializer.ErrorHandlingDeserializer;
import org.springframework.kafka.support.serializer.JsonDeserializer;
import org.springframework.kafka.support.serializer.JsonSerializer;
import org.springframework.util.backoff.FixedBackOff;

import java.util.HashMap;
import java.util.Map;

/**
 * Kafka consumer configuration for the audit service.
 * <p>
 * One listener container factory is defined per event type so each
 * topic's messages are deserialized to the correct domain event type.
 * </p>
 * <p>
 * A {@link DefaultErrorHandler} with a {@link DeadLetterPublishingRecoverer} is
 * attached to all factories. Consumers retry transient failures up to
 * {@code aegis.kafka.retry.max-attempts} times with a fixed back-off; when the
 * attempts are exhausted the failed record is published to the dead letter
 * topic (<code>&lt;topic&gt;.dlt</code>).</p>
 */
@Configuration
@EnableKafka
public class KafkaConfig {

    @Value("${aegis.kafka.retry.max-attempts:3}")
    private long maxAttempts;

    @Value("${aegis.kafka.retry.backoff-ms:1000}")
    private long backoffMs;

    @Value("${aegis.kafka.dlt-suffix:.dlt}")
    private String dltSuffix;

    /**
     * Creates a consumer factory that deserializes messages to
     * {@link FundsDepositedEvent} (the wallet deposit topic has no type headers).
     *
     * @return the consumer factory
     */
    @Bean
    public ConsumerFactory<String, FundsDepositedEvent> fundsDepositedConsumerFactory(KafkaProperties properties) {
        Map<String, Object> props = baseProps(properties);
        props.put(JsonDeserializer.VALUE_DEFAULT_TYPE, FundsDepositedEvent.class.getName());
        return new DefaultKafkaConsumerFactory<>(props);
    }

    /**
     * Creates a consumer factory that deserializes messages to
     * {@link FraudAssessmentCompletedEvent} (the fraud topic has no type headers).
     *
     * @return the consumer factory
     */
    @Bean
    public ConsumerFactory<String, FraudAssessmentCompletedEvent> fraudAssessmentConsumerFactory(KafkaProperties properties) {
        Map<String, Object> props = baseProps(properties);
        props.put(JsonDeserializer.VALUE_DEFAULT_TYPE, FraudAssessmentCompletedEvent.class.getName());
        return new DefaultKafkaConsumerFactory<>(props);
    }

    /**
     * Creates a consumer factory that deserializes messages to
     * {@link TransferRequestedEvent}.
     *
     * @return the consumer factory
     */
    @Bean
    public ConsumerFactory<String, TransferRequestedEvent> transferRequestedConsumerFactory(KafkaProperties properties) {
        Map<String, Object> props = baseProps(properties);
        props.put(JsonDeserializer.VALUE_DEFAULT_TYPE, TransferRequestedEvent.class.getName());
        return new DefaultKafkaConsumerFactory<>(props);
    }

    /**
     * Creates a consumer factory that deserializes messages to
     * {@link TransferCompletedEvent}.
     *
     * @return the consumer factory
     */
    @Bean
    public ConsumerFactory<String, TransferCompletedEvent> transferCompletedConsumerFactory(KafkaProperties properties) {
        Map<String, Object> props = baseProps(properties);
        props.put(JsonDeserializer.VALUE_DEFAULT_TYPE, TransferCompletedEvent.class.getName());
        return new DefaultKafkaConsumerFactory<>(props);
    }

    /**
     * Creates a consumer factory that deserializes messages to
     * {@link TransferFailedEvent}.
     *
     * @return the consumer factory
     */
    @Bean
    public ConsumerFactory<String, TransferFailedEvent> transferFailedConsumerFactory(KafkaProperties properties) {
        Map<String, Object> props = baseProps(properties);
        props.put(JsonDeserializer.VALUE_DEFAULT_TYPE, TransferFailedEvent.class.getName());
        return new DefaultKafkaConsumerFactory<>(props);
    }

    /**
     * Creates a consumer factory that deserializes messages to
     * {@link PaymentRequestedEvent}.
     *
     * @return the consumer factory
     */
    @Bean
    public ConsumerFactory<String, PaymentRequestedEvent> paymentRequestedConsumerFactory(KafkaProperties properties) {
        Map<String, Object> props = baseProps(properties);
        props.put(JsonDeserializer.VALUE_DEFAULT_TYPE, PaymentRequestedEvent.class.getName());
        return new DefaultKafkaConsumerFactory<>(props);
    }

    /**
     * Creates a consumer factory that deserializes messages to
     * {@link PaymentExecutedEvent}.
     *
     * @return the consumer factory
     */
    @Bean
    public ConsumerFactory<String, PaymentExecutedEvent> paymentExecutedConsumerFactory(KafkaProperties properties) {
        Map<String, Object> props = baseProps(properties);
        props.put(JsonDeserializer.VALUE_DEFAULT_TYPE, PaymentExecutedEvent.class.getName());
        return new DefaultKafkaConsumerFactory<>(props);
    }

    /**
     * Creates a consumer factory that deserializes messages to
     * {@link PaymentFailedEvent}.
     *
     * @return the consumer factory
     */
    @Bean
    public ConsumerFactory<String, PaymentFailedEvent> paymentFailedConsumerFactory(KafkaProperties properties) {
        Map<String, Object> props = baseProps(properties);
        props.put(JsonDeserializer.VALUE_DEFAULT_TYPE, PaymentFailedEvent.class.getName());
        return new DefaultKafkaConsumerFactory<>(props);
    }

    /**
     * Listener container factory for the funds deposited topic.
     *
     * @return the container factory
     */
    @Bean
    public ConcurrentKafkaListenerContainerFactory<String, FundsDepositedEvent>
            fundsDepositedListenerContainerFactory(KafkaProperties properties,
                                                   KafkaTemplate<String, Object> kafkaTemplate) {
        ConcurrentKafkaListenerContainerFactory<String, FundsDepositedEvent> factory =
                new ConcurrentKafkaListenerContainerFactory<>();
        factory.setConsumerFactory(fundsDepositedConsumerFactory(properties));
        factory.setCommonErrorHandler(commonErrorHandler(kafkaTemplate));
        return factory;
    }

    /**
     * Listener container factory for the fraud assessment topic.
     *
     * @return the container factory
     */
    @Bean
    public ConcurrentKafkaListenerContainerFactory<String, FraudAssessmentCompletedEvent>
            fraudAssessmentListenerContainerFactory(KafkaProperties properties,
                                                    KafkaTemplate<String, Object> kafkaTemplate) {
        ConcurrentKafkaListenerContainerFactory<String, FraudAssessmentCompletedEvent> factory =
                new ConcurrentKafkaListenerContainerFactory<>();
        factory.setConsumerFactory(fraudAssessmentConsumerFactory(properties));
        factory.setCommonErrorHandler(commonErrorHandler(kafkaTemplate));
        return factory;
    }

    /**
     * Listener container factory for the transfer requested topic.
     *
     * @return the container factory
     */
    @Bean
    public ConcurrentKafkaListenerContainerFactory<String, TransferRequestedEvent>
            transferRequestedListenerContainerFactory(KafkaProperties properties,
                                                      KafkaTemplate<String, Object> kafkaTemplate) {
        ConcurrentKafkaListenerContainerFactory<String, TransferRequestedEvent> factory =
                new ConcurrentKafkaListenerContainerFactory<>();
        factory.setConsumerFactory(transferRequestedConsumerFactory(properties));
        factory.setCommonErrorHandler(commonErrorHandler(kafkaTemplate));
        return factory;
    }

    /**
     * Listener container factory for the transfer completed topic.
     *
     * @return the container factory
     */
    @Bean
    public ConcurrentKafkaListenerContainerFactory<String, TransferCompletedEvent>
            transferCompletedListenerContainerFactory(KafkaProperties properties,
                                                      KafkaTemplate<String, Object> kafkaTemplate) {
        ConcurrentKafkaListenerContainerFactory<String, TransferCompletedEvent> factory =
                new ConcurrentKafkaListenerContainerFactory<>();
        factory.setConsumerFactory(transferCompletedConsumerFactory(properties));
        factory.setCommonErrorHandler(commonErrorHandler(kafkaTemplate));
        return factory;
    }

    /**
     * Listener container factory for the transfer failed topic.
     *
     * @return the container factory
     */
    @Bean
    public ConcurrentKafkaListenerContainerFactory<String, TransferFailedEvent>
            transferFailedListenerContainerFactory(KafkaProperties properties,
                                                   KafkaTemplate<String, Object> kafkaTemplate) {
        ConcurrentKafkaListenerContainerFactory<String, TransferFailedEvent> factory =
                new ConcurrentKafkaListenerContainerFactory<>();
        factory.setConsumerFactory(transferFailedConsumerFactory(properties));
        factory.setCommonErrorHandler(commonErrorHandler(kafkaTemplate));
        return factory;
    }

    /**
     * Listener container factory for the payment requested topic.
     *
     * @return the container factory
     */
    @Bean
    public ConcurrentKafkaListenerContainerFactory<String, PaymentRequestedEvent>
            paymentRequestedListenerContainerFactory(KafkaProperties properties,
                                                     KafkaTemplate<String, Object> kafkaTemplate) {
        ConcurrentKafkaListenerContainerFactory<String, PaymentRequestedEvent> factory =
                new ConcurrentKafkaListenerContainerFactory<>();
        factory.setConsumerFactory(paymentRequestedConsumerFactory(properties));
        factory.setCommonErrorHandler(commonErrorHandler(kafkaTemplate));
        return factory;
    }

    /**
     * Listener container factory for the payment executed topic.
     *
     * @return the container factory
     */
    @Bean
    public ConcurrentKafkaListenerContainerFactory<String, PaymentExecutedEvent>
            paymentExecutedListenerContainerFactory(KafkaProperties properties,
                                                    KafkaTemplate<String, Object> kafkaTemplate) {
        ConcurrentKafkaListenerContainerFactory<String, PaymentExecutedEvent> factory =
                new ConcurrentKafkaListenerContainerFactory<>();
        factory.setConsumerFactory(paymentExecutedConsumerFactory(properties));
        factory.setCommonErrorHandler(commonErrorHandler(kafkaTemplate));
        return factory;
    }

    /**
     * Listener container factory for the payment failed topic.
     *
     * @return the container factory
     */
    @Bean
    public ConcurrentKafkaListenerContainerFactory<String, PaymentFailedEvent>
            paymentFailedListenerContainerFactory(KafkaProperties properties,
                                                  KafkaTemplate<String, Object> kafkaTemplate) {
        ConcurrentKafkaListenerContainerFactory<String, PaymentFailedEvent> factory =
                new ConcurrentKafkaListenerContainerFactory<>();
        factory.setConsumerFactory(paymentFailedConsumerFactory(properties));
        factory.setCommonErrorHandler(commonErrorHandler(kafkaTemplate));
        return factory;
    }

    /**
     * Producer factory used by the dead letter recoverer. The value serializer
     * must be a {@link JsonSerializer} so the recovered (deserialized) value
     * can be re-published to the dead letter topic; the default Spring Boot
     * producer uses a {@code StringSerializer}, which cannot serialize the
     * deserialized domain event.
     *
     * @param properties the autoconfigured Kafka properties
     * @return the producer factory
     */
    @Bean
    public ProducerFactory<String, Object> dltProducerFactory(KafkaProperties properties) {
        Map<String, Object> props = new HashMap<>(properties.buildProducerProperties());
        props.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class);
        props.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, JsonSerializer.class);
        return new DefaultKafkaProducerFactory<>(props);
    }

    /**
     * Kafka producer template used by the dead letter recoverer.
     *
     * @param dltProducerFactory the producer factory for DLT publication
     * @return the template
     */
    @Bean
    public KafkaTemplate<String, Object> kafkaTemplate(ProducerFactory<String, Object> dltProducerFactory) {
        return new KafkaTemplate<>(dltProducerFactory);
    }

    /**
     * Builds the error handler shared by all listener factories: retry with a
     * fixed back-off, then publish the failed record to the topic's dead letter
     * topic.
     *
     * @param kafkaTemplate the producer template for the DLT
     * @return the configured error handler
     */
    @Bean
    public DefaultErrorHandler commonErrorHandler(KafkaTemplate<String, Object> kafkaTemplate) {
        DeadLetterPublishingRecoverer recoverer = new DeadLetterPublishingRecoverer(
                kafkaTemplate,
                (record, ex) -> new org.apache.kafka.common.TopicPartition(
                        record.topic() + dltSuffix, record.partition()));
        return new DefaultErrorHandler(recoverer, new FixedBackOff(backoffMs, maxAttempts - 1));
    }

    private Map<String, Object> baseProps(KafkaProperties properties) {
        Map<String, Object> props = new HashMap<>(properties.buildConsumerProperties());
        props.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, ErrorHandlingDeserializer.class);
        props.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, ErrorHandlingDeserializer.class);
        props.put(ErrorHandlingDeserializer.KEY_DESERIALIZER_CLASS, StringDeserializer.class);
        props.put(ErrorHandlingDeserializer.VALUE_DESERIALIZER_CLASS, JsonDeserializer.class);
        props.put(JsonDeserializer.TRUSTED_PACKAGES, "com.aegis.audit.domain.event");
        props.put(JsonDeserializer.USE_TYPE_INFO_HEADERS, false);
        return props;
    }
}
