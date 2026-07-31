package com.aegis.fraud.domain.port.outbound;

import com.aegis.fraud.domain.event.FraudAssessmentCompleted;

/**
 * Port for publishing fraud domain events to the messaging infrastructure.
 */
public interface EventPublisher {

    void publish(FraudAssessmentCompleted event);
}
