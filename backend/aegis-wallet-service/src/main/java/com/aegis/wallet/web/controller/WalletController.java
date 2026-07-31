package com.aegis.wallet.web.controller;

import com.aegis.wallet.application.dto.CreateWalletCommand;
import com.aegis.wallet.application.dto.DepositReceipt;
import com.aegis.wallet.application.dto.WalletDetailResponse;
import com.aegis.wallet.application.dto.WalletResponse;
import com.aegis.wallet.application.service.CreateWalletService;
import com.aegis.wallet.application.service.DepositFundsService;
import com.aegis.wallet.application.service.UpdateWalletService;
import com.aegis.wallet.domain.model.WalletStatus;
import com.aegis.wallet.domain.port.inbound.DepositFundsUseCase;
import com.aegis.wallet.domain.port.inbound.GetWalletDetailUseCase;
import com.aegis.wallet.domain.port.inbound.ListWalletsUseCase;
import com.aegis.wallet.domain.port.inbound.UpdateWalletUseCase;
import com.aegis.wallet.web.dto.AdjustBalanceRequest;
import com.aegis.wallet.web.dto.CreateWalletRequest;
import com.aegis.wallet.web.dto.DepositFundsRequest;
import com.aegis.wallet.web.dto.UpdateStatusRequest;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/wallets")
public class WalletController {

    private final CreateWalletService createWalletService;
    private final UpdateWalletService updateWalletService;
    private final DepositFundsService depositFundsService;
    private final ListWalletsUseCase listWalletsUseCase;
    private final GetWalletDetailUseCase getWalletDetailUseCase;

    public WalletController(CreateWalletService createWalletService,
                             UpdateWalletService updateWalletService,
                             DepositFundsService depositFundsService,
                             ListWalletsUseCase listWalletsUseCase,
                             GetWalletDetailUseCase getWalletDetailUseCase) {
        this.createWalletService = createWalletService;
        this.updateWalletService = updateWalletService;
        this.depositFundsService = depositFundsService;
        this.listWalletsUseCase = listWalletsUseCase;
        this.getWalletDetailUseCase = getWalletDetailUseCase;
    }

    @PostMapping
    public ResponseEntity<WalletResponse> createWallet(
            @Valid @RequestBody CreateWalletRequest request,
            @RequestHeader("X-User-Id") UUID userId,
            @RequestHeader(value = "X-Correlation-Id", required = false) String correlationId) {

        String effectiveCorrelationId = correlationId != null
                ? correlationId
                : UUID.randomUUID().toString();

        CreateWalletCommand command = new CreateWalletCommand(
                request.currency(),
                effectiveCorrelationId
        );

        WalletResponse response = createWalletService.createAndReturnResponse(command, userId);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping
    public ResponseEntity<List<WalletResponse>> listWallets(
            @RequestHeader("X-User-Id") UUID userId) {

        List<ListWalletsUseCase.Result> results = listWalletsUseCase.listByUser(userId);
        List<WalletResponse> responses = results.stream()
                .map(r -> new WalletResponse(
                        r.walletId(), r.userId(), r.balance(), r.currency(),
                        r.status(), r.premium(), r.createdAt()))
                .toList();
        return ResponseEntity.ok(responses);
    }

    @GetMapping("/{walletId}")
    public ResponseEntity<WalletDetailResponse> getWallet(
            @PathVariable UUID walletId,
            @RequestHeader("X-User-Id") UUID userId) {

        GetWalletDetailUseCase.Result result = getWalletDetailUseCase.getDetail(walletId, userId);
        return ResponseEntity.ok(new WalletDetailResponse(
                result.walletId(), result.userId(), result.balance(), result.currency(),
                result.status(), result.premium(), result.createdAt(), result.updatedAt()));
    }

    @PatchMapping("/{walletId}/balance")
    public ResponseEntity<WalletDetailResponse> adjustBalance(
            @PathVariable UUID walletId,
            @RequestHeader("X-User-Id") UUID userId,
            @RequestHeader(value = "X-Correlation-Id", required = false) String correlationId,
            @Valid @RequestBody AdjustBalanceRequest request) {

        String effectiveCorrelationId = correlationId != null
                ? correlationId
                : UUID.randomUUID().toString();

        var result = updateWalletService.adjustBalance(new UpdateWalletUseCase.AdjustBalanceCommand(
                walletId, userId, request.amount(), request.description(), effectiveCorrelationId));

        return ResponseEntity.ok(toDetailResponse(result));
    }

    @PostMapping("/{walletId}/deposits")
    public ResponseEntity<DepositReceipt> depositFunds(
            @PathVariable UUID walletId,
            @RequestHeader("X-User-Id") UUID userId,
            @RequestHeader(value = "X-Correlation-Id", required = false) String correlationId,
            @Valid @RequestBody DepositFundsRequest request) {

        String effectiveCorrelationId = correlationId != null
                ? correlationId
                : UUID.randomUUID().toString();

        var result = depositFundsService.deposit(new DepositFundsUseCase.DepositCommand(
                walletId, userId, request.amount(), request.currency(),
                request.source(), request.reference(), effectiveCorrelationId));

        return ResponseEntity.status(HttpStatus.CREATED).body(new DepositReceipt(
                result.depositId(),
                result.walletId(),
                result.newBalance(),
                result.amount(),
                result.currency(),
                result.source(),
                result.reference(),
                result.timestamp()
        ));
    }

    @PatchMapping("/{walletId}/status")
    public ResponseEntity<WalletDetailResponse> updateStatus(
            @PathVariable UUID walletId,
            @RequestHeader("X-User-Id") UUID userId,
            @Valid @RequestBody UpdateStatusRequest request) {

        WalletStatus newStatus = WalletStatus.fromString(request.status());

        var result = updateWalletService.changeStatus(new UpdateWalletUseCase.StatusChangeCommand(
                walletId, userId, newStatus));

        return ResponseEntity.ok(toDetailResponse(result));
    }

    private WalletDetailResponse toDetailResponse(UpdateWalletUseCase.WalletDetailResult result) {
        return new WalletDetailResponse(
                result.walletId(),
                result.userId(),
                result.balance(),
                result.currency(),
                result.status(),
                result.premium(),
                result.createdAt(),
                result.updatedAt()
        );
    }
}
