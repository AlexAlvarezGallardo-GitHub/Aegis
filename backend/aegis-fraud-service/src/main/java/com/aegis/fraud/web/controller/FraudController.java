package com.aegis.fraud.web.controller;

import com.aegis.fraud.application.dto.AssessmentResponse;
import com.aegis.fraud.application.dto.FraudAssessmentCommand;
import com.aegis.fraud.web.mapper.AssessmentMapper;
import com.aegis.fraud.domain.port.inbound.AssessFraudUseCase;
import com.aegis.fraud.web.dto.AssessmentRequest;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

/**
 * REST controller for fraud assessment operations.
 */
@RestController
@RequestMapping("/api/v1/fraud")
public class FraudController {

    private final AssessFraudUseCase assessFraudUseCase;

    public FraudController(AssessFraudUseCase assessFraudUseCase) {
        this.assessFraudUseCase = assessFraudUseCase;
    }

    /**
     * Assesses a transaction for fraud.
     *
     * @param request the validated assessment request
     * @return the assessment result
     */
    @PostMapping("/assess")
    public ResponseEntity<AssessmentResponse> assess(@Valid @RequestBody AssessmentRequest request) {
        FraudAssessmentCommand command = AssessmentMapper.toCommand(request);
        var assessment = assessFraudUseCase.assess(AssessmentMapper.toUseCaseCommand(command));
        return ResponseEntity.ok(AssessmentResponse.from(assessment));
    }

    /**
     * Retrieves a fraud assessment by its identifier.
     *
     * @param assessmentId the assessment identifier
     * @return the assessment result
     */
    @GetMapping("/assessments/{assessmentId}")
    public ResponseEntity<AssessmentResponse> getAssessment(@PathVariable UUID assessmentId) {
        var assessment = assessFraudUseCase.findById(assessmentId);
        return ResponseEntity.ok(AssessmentResponse.from(assessment));
    }
}
