package com.aegis.fraud.application.service;

import com.aegis.fraud.domain.model.RuleEvaluation;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class RiskScorer {

    private static final int MAX_SCORE = 100;

    public int score(List<RuleEvaluation> evaluations) {
        int total = evaluations.stream()
                .mapToInt(RuleEvaluation::score)
                .sum();
        return Math.min(MAX_SCORE, total);
    }
}
