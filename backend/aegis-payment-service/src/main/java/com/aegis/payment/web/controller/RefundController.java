package com.aegis.payment.web.controller;

import com.aegis.payment.application.dto.RefundResult;
import com.aegis.payment.domain.port.inbound.RefundPaymentUseCase;
import com.aegis.payment.web.dto.RefundRequest;
import com.aegis.payment.web.dto.RefundResponse;
import com.aegis.payment.web.mapper.RefundMapper;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

/**
 * REST controller for refund operations.
 */
@RestController
@RequestMapping("/api/v1/payments")
public class RefundController {

    private final RefundPaymentUseCase refundPaymentUseCase;

    public RefundController(RefundPaymentUseCase refundPaymentUseCase) {
        this.refundPaymentUseCase = refundPaymentUseCase;
    }

    /**
     * Refunds a completed payment.
     *
     * <p>The authenticated user id is taken from the {@code X-User-Id} header
     * (populated by the BFF from the session JWT) — never from the request body,
     * so a caller cannot refund another user's payment.</p>
     *
     * @param paymentId     the payment to refund
     * @param request       the validated refund request
     * @param userId        the authenticated user id (X-User-Id header)
     * @param adminOverride optional admin override flag
     * @return the completed or failed refund
     */
    @PostMapping("/{paymentId}/refund")
    public ResponseEntity<RefundResponse> refundPayment(
            @PathVariable UUID paymentId,
            @Valid @RequestBody RefundRequest request,
            @RequestHeader("X-User-Id") UUID userId,
            @RequestHeader(value = "X-Admin-Override", required = false, defaultValue = "false") boolean adminOverride) {

        RefundPaymentUseCase.RefundCommand command = RefundMapper.toCommand(paymentId, request, userId, adminOverride);
        RefundResult result = refundPaymentUseCase.refund(command);
        RefundResponse response = RefundResponse.from(result);
        return ResponseEntity.ok(response);
    }
}
