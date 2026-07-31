package com.aegis.fraud.domain.port.outbound;

import com.aegis.fraud.domain.model.FraudAssessment;

import java.util.Optional;
import java.util.UUID;

public interface FraudAssessmentRepository {

    FraudAssessment save(FraudAssessment assessment);

    Optional<FraudAssessment> findById(UUID assessmentId);
}
