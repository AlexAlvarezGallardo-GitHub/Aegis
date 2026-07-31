package com.aegis.fraud.infrastructure.persistence;

import com.aegis.fraud.domain.model.FraudRule;
import com.aegis.fraud.domain.port.outbound.FraudRuleRepository;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class FraudRuleRepositoryAdapter implements FraudRuleRepository {

    private final FraudRuleJpaRepository jpaRepository;

    public FraudRuleRepositoryAdapter(FraudRuleJpaRepository jpaRepository) {
        this.jpaRepository = jpaRepository;
    }

    @Override
    public List<FraudRule> findEnabledRules() {
        return jpaRepository.findByEnabledTrue().stream()
                .map(entity -> new FraudRule(
                        entity.getId(),
                        entity.getName(),
                        FraudRule.RuleType.valueOf(entity.getType().name()),
                        entity.getThreshold(),
                        entity.getWeight(),
                        entity.isEnabled()))
                .toList();
    }
}
