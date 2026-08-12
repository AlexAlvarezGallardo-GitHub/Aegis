package com.aegis.payment.web.controller;

import com.aegis.payment.application.dto.PaymentResult;
import com.aegis.payment.domain.model.Payment;
import com.aegis.payment.domain.port.inbound.ExecutePaymentUseCase;
import com.aegis.payment.domain.port.inbound.GetPaymentUseCase;
import com.aegis.payment.web.dto.PaymentRequest;
import com.aegis.payment.web.dto.PaymentResponse;
import com.aegis.payment.web.mapper.PaymentMapper;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.net.URI;
import java.util.UUID;

/**
 * REST controller for payment operations.
 */
@RestController
@RequestMapping("/api/v1/payments")
public class PaymentController {

    private final ExecutePaymentUseCase executePaymentUseCase;
    private final GetPaymentUseCase getPaymentUseCase;

    public PaymentController(ExecutePaymentUseCase executePaymentUseCase,
                             GetPaymentUseCase getPaymentUseCase) {
        this.executePaymentUseCase = executePaymentUseCase;
        this.getPaymentUseCase = getPaymentUseCase;
    }

    /**
     * Executes a new payment.
     *
     * @param request the validated payment request
     * @return the completed or failed payment
     */
    @PostMapping
    public ResponseEntity<PaymentResponse> executePayment(@Valid @RequestBody PaymentRequest request) {
        ExecutePaymentUseCase.PaymentCommand command = PaymentMapper.toCommand(request);
        Payment payment = executePaymentUseCase.execute(command);
        PaymentResponse response = PaymentResponse.from(PaymentResult.from(payment));
        return ResponseEntity.created(URI.create("/api/v1/payments/" + payment.getId())).body(response);
    }

    /**
     * Retrieves a payment by its identifier.
     *
     * @param paymentId the payment identifier
     * @return the payment
     */
    @GetMapping("/{paymentId}")
    public ResponseEntity<PaymentResponse> getPayment(@PathVariable UUID paymentId) {
        Payment payment = getPaymentUseCase.findById(paymentId);
        return ResponseEntity.ok(PaymentResponse.from(PaymentResult.from(payment)));
    }
}
