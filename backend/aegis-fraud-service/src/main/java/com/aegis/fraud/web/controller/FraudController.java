package com.aegis.fraud.web.controller;

import com.aegis.fraud.application.dto.AssessmentRequest;
import com.aegis.fraud.application.dto.AssessmentResponse;
import com.aegis.fraud.domain.port.inbound.AssessFraudUseCase;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/fraud")
public class FraudController {

    private final AssessFraudUseCase assessFraudUseCase;

    public FraudController(AssessFraudUseCase assessFraudUseCase) {
        this.assessFraudUseCase = assessFraudUseCase;
    }

    @PostMapping("/assess")
    public ResponseEntity<AssessmentResponse> assess(@Valid @RequestBody AssessmentRequest request) {
        var command = new AssessFraudUseCase.AssessmentCommand(
                request.transactionId(),
                request.transactionType(),
                request.amount(),
                request.currency(),
                request.sourceWalletId(),
                request.destWalletId(),
                request.userId(),
                request.countryCode());

        var assessment = assessFraudUseCase.assess(command);
        return ResponseEntity.ok(AssessmentResponse.from(assessment));
    }

    @GetMapping("/assessments/{assessmentId}")
    public ResponseEntity<AssessmentResponse> getAssessment(@PathVariable UUID assessmentId) {
        var assessment = assessFraudUseCase.findById(assessmentId);
        return ResponseEntity.ok(AssessmentResponse.from(assessment));
    }
}
