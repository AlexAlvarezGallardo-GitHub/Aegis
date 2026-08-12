package com.aegis.wallet.web.controller;

import com.aegis.wallet.domain.port.inbound.CreateHoldUseCase;
import com.aegis.wallet.domain.port.inbound.DebitHoldUseCase;
import com.aegis.wallet.domain.port.inbound.ReleaseHoldUseCase;
import com.aegis.wallet.domain.port.inbound.SettleTransferUseCase;
import com.aegis.wallet.web.dto.DebitHoldRequest;
import com.aegis.wallet.web.dto.DebitHoldResponse;
import com.aegis.wallet.web.dto.HoldRequest;
import com.aegis.wallet.web.dto.HoldResponse;
import com.aegis.wallet.web.dto.SettleTransferRequest;
import com.aegis.wallet.web.dto.SettleTransferResponse;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/wallets")
public class HoldController {

    private final CreateHoldUseCase createHoldUseCase;
    private final SettleTransferUseCase settleTransferUseCase;
    private final ReleaseHoldUseCase releaseHoldUseCase;
    private final DebitHoldUseCase debitHoldUseCase;

    public HoldController(CreateHoldUseCase createHoldUseCase,
                          SettleTransferUseCase settleTransferUseCase,
                          ReleaseHoldUseCase releaseHoldUseCase,
                          DebitHoldUseCase debitHoldUseCase) {
        this.createHoldUseCase = createHoldUseCase;
        this.settleTransferUseCase = settleTransferUseCase;
        this.releaseHoldUseCase = releaseHoldUseCase;
        this.debitHoldUseCase = debitHoldUseCase;
    }

    @PostMapping("/{walletId}/holds")
    public ResponseEntity<HoldResponse> createHold(
            @PathVariable UUID walletId,
            @Valid @RequestBody HoldRequest request) {

        CreateHoldUseCase.HoldResult result = createHoldUseCase.createHold(
                new CreateHoldUseCase.CreateHoldCommand(
                        walletId, request.amount(), request.currency(), request.reference()));

        return ResponseEntity.status(HttpStatus.CREATED).body(toHoldResponse(result));
    }

    @PostMapping("/transfers/settle")
    public ResponseEntity<SettleTransferResponse> settleTransfer(
            @Valid @RequestBody SettleTransferRequest request) {

        SettleTransferUseCase.SettleResult result = settleTransferUseCase.settle(
                new SettleTransferUseCase.SettleCommand(
                        request.transferId(), request.holdId(),
                        request.sourceWalletId(), request.destWalletId(),
                        request.amount(), request.currency()));

        return ResponseEntity.ok(new SettleTransferResponse(
                result.transferId(), result.holdId(),
                result.sourceWalletId(), result.sourceNewBalance(),
                result.destWalletId(), result.destNewBalance(),
                result.timestamp()));
    }

    @PostMapping("/{walletId}/holds/{holdId}/release")
    public ResponseEntity<HoldResponse> releaseHold(
            @PathVariable UUID walletId,
            @PathVariable UUID holdId) {

        ReleaseHoldUseCase.HoldResult result = releaseHoldUseCase.release(
                new ReleaseHoldUseCase.ReleaseCommand(walletId, holdId));

        return ResponseEntity.ok(toHoldResponse(result));
    }

    @PostMapping("/{walletId}/holds/{holdId}/debit")
    public ResponseEntity<DebitHoldResponse> debitHold(
            @PathVariable UUID walletId,
            @PathVariable UUID holdId,
            @Valid @RequestBody DebitHoldRequest request) {

        DebitHoldUseCase.DebitResult result = debitHoldUseCase.debit(
                new DebitHoldUseCase.DebitCommand(
                        request.paymentId(), request.holdId(),
                        walletId, request.amount(), request.currency()));

        return ResponseEntity.ok(new DebitHoldResponse(
                result.paymentId(), result.holdId(),
                result.walletId(), result.newBalance(),
                result.timestamp()));
    }

    private HoldResponse toHoldResponse(CreateHoldUseCase.HoldResult result) {
        return new HoldResponse(
                result.holdId(), result.walletId(), result.amount(), result.currency(),
                result.reference(), result.status(), result.availableBalance(),
                result.createdAt(), result.expiresAt());
    }

    private HoldResponse toHoldResponse(ReleaseHoldUseCase.HoldResult result) {
        return new HoldResponse(
                result.holdId(), result.walletId(), result.amount(), result.currency(),
                result.reference(), result.status(), result.availableBalance(),
                result.createdAt(), result.expiresAt());
    }
}
