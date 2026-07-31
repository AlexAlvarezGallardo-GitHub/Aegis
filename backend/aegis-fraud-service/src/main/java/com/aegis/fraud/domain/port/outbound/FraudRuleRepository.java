package com.aegis.fraud.domain.port.outbound;

import com.aegis.fraud.domain.model.FraudRule;

import java.util.List;

public interface FraudRuleRepository {

    List<FraudRule> findEnabledRules();
}
