package com.aegis.fraud.domain.exception;

import com.aegis.common.domain.exception.AegisException;
import com.aegis.common.domain.exception.ErrorStatus;

import java.util.UUID;

public class AssessmentNotFoundException extends AegisException {

    private static final String CODE = "ASSESSMENT_NOT_FOUND";

    public AssessmentNotFoundException(UUID assessmentId) {
        super(CODE, ErrorStatus.NOT_FOUND, "Fraud assessment not found: " + assessmentId);
    }
}
