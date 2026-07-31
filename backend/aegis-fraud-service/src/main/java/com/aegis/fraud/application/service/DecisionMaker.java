package com.aegis.fraud.application.service;

import com.aegis.fraud.domain.model.FraudDecision;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class DecisionMaker {

    private final int reviewThreshold;
    private final int rejectThreshold;

    public DecisionMaker(@Value("${aegis.fraud.review-threshold:30}") int reviewThreshold,
                         @Value("${aegis.fraud.reject-threshold:70}") int rejectThreshold) {
        this.reviewThreshold = reviewThreshold;
        this.rejectThreshold = rejectThreshold;
    }

    public FraudDecision decide(int riskScore) {
        if (riskScore < reviewThreshold) {
            return FraudDecision.APPROVE;
        }
        if (riskScore > rejectThreshold) {
            return FraudDecision.REJECT;
        }
        return FraudDecision.REVIEW;
    }
}
