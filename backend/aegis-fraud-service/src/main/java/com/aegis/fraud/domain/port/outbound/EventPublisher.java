package com.aegis.fraud.domain.port.outbound;

import com.aegis.fraud.domain.event.FraudAssessmentCompleted;

public interface EventPublisher {

    void publish(FraudAssessmentCompleted event);
}
