package com.aegis.fraud.domain.model;

public record RuleEvaluation(String ruleName, int score, boolean matched, String details) {}
