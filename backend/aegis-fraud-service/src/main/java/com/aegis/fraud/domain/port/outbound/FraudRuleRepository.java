package com.aegis.fraud.domain.port.outbound;

import com.aegis.fraud.domain.model.FraudRule;

import java.util.List;

/**
 * Port for retrieving enabled fraud rules.
 */
public interface FraudRuleRepository {

    List<FraudRule> findEnabledRules();
}
