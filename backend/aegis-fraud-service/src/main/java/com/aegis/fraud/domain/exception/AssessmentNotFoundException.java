package com.aegis.fraud.domain.exception;

import com.aegis.common.domain.exception.AegisException;

import java.util.UUID;

public class AssessmentNotFoundException extends AegisException {

    private static final String CODE = "ASSESSMENT_NOT_FOUND";

    public AssessmentNotFoundException(UUID assessmentId) {
        super(CODE, "Fraud assessment not found: " + assessmentId);
    }
}
