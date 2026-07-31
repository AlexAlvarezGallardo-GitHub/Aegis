package com.aegis.fraud.infrastructure.messaging;

import com.aegis.fraud.domain.event.FraudAssessmentCompleted;
import com.aegis.fraud.domain.port.outbound.EventPublisher;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

@Component
public class KafkaEventPublisher implements EventPublisher {

    private static final Logger log = LoggerFactory.getLogger(KafkaEventPublisher.class);

    private final KafkaTemplate<String, Object> kafkaTemplate;
    private final String topic;

    public KafkaEventPublisher(KafkaTemplate<String, Object> kafkaTemplate,
                               @Value("${aegis.kafka.topics.fraud-assessment-completed:fraud.assessment.completed}") String topic) {
        this.kafkaTemplate = kafkaTemplate;
        this.topic = topic;
    }

    @Override
    public void publish(FraudAssessmentCompleted event) {
        log.info("Publishing FraudAssessmentCompleted for transaction {} to {}",
                event.transactionId(), topic);
        kafkaTemplate.send(topic, event.transactionId().toString(), event)
                .whenComplete((result, ex) -> {
                    if (ex != null) {
                        log.error("Failed to publish FraudAssessmentCompleted for transaction {}",
                                event.transactionId(), ex);
                    }
                });
    }
}
