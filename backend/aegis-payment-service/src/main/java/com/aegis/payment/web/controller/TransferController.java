package com.aegis.payment.web.controller;

import com.aegis.payment.application.dto.TransferResult;
import com.aegis.payment.domain.model.Transfer;
import com.aegis.payment.domain.port.inbound.GetTransferUseCase;
import com.aegis.payment.domain.port.inbound.TransferFundsUseCase;
import com.aegis.payment.web.dto.TransferRequest;
import com.aegis.payment.web.dto.TransferResponse;
import com.aegis.payment.web.mapper.TransferMapper;
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
 * REST controller for transfer operations.
 */
@RestController
@RequestMapping("/api/v1/transfers")
public class TransferController {

    private final TransferFundsUseCase transferFundsUseCase;
    private final GetTransferUseCase getTransferUseCase;

    public TransferController(TransferFundsUseCase transferFundsUseCase,
                              GetTransferUseCase getTransferUseCase) {
        this.transferFundsUseCase = transferFundsUseCase;
        this.getTransferUseCase = getTransferUseCase;
    }

    /**
     * Initiates a new funds transfer.
     *
     * @param request the validated transfer request
     * @return the created transfer
     */
    @PostMapping
    public ResponseEntity<TransferResponse> createTransfer(@Valid @RequestBody TransferRequest request) {
        TransferFundsUseCase.TransferCommand command = TransferMapper.toCommand(request);
        Transfer transfer = transferFundsUseCase.execute(command);
        TransferResponse response = TransferResponse.from(TransferResult.from(transfer));
        return ResponseEntity.created(URI.create("/api/v1/transfers/" + transfer.getId())).body(response);
    }

    /**
     * Retrieves a transfer by its identifier.
     *
     * @param transferId the transfer identifier
     * @return the transfer
     */
    @GetMapping("/{transferId}")
    public ResponseEntity<TransferResponse> getTransfer(@PathVariable UUID transferId) {
        Transfer transfer = getTransferUseCase.findById(transferId);
        return ResponseEntity.ok(TransferResponse.from(TransferResult.from(transfer)));
    }
}
