package com.aegis.wallet.web.controller;

import com.aegis.wallet.application.dto.AdjustBalanceCommand;
import com.aegis.wallet.application.dto.CreateWalletCommand;
import com.aegis.wallet.application.dto.DepositFundsCommand;
import com.aegis.wallet.application.dto.DepositReceipt;
import com.aegis.wallet.application.dto.UpdateStatusCommand;
import com.aegis.wallet.application.dto.WalletDetailResponse;
import com.aegis.wallet.application.dto.WalletResponse;
import com.aegis.wallet.application.mapper.WalletMapper;
import com.aegis.wallet.application.service.CreateWalletService;
import com.aegis.wallet.application.service.DepositFundsService;
import com.aegis.wallet.application.service.UpdateWalletService;
import com.aegis.wallet.domain.exception.WalletNotFoundException;
import com.aegis.wallet.domain.model.WalletId;
import com.aegis.wallet.domain.model.WalletStatus;
import com.aegis.wallet.domain.port.inbound.DepositFundsUseCase;
import com.aegis.wallet.domain.port.inbound.UpdateWalletUseCase;
import com.aegis.wallet.domain.port.outbound.WalletRepository;
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
    private final WalletRepository walletRepository;

    public WalletController(CreateWalletService createWalletService,
                             UpdateWalletService updateWalletService,
                             DepositFundsService depositFundsService,
                             WalletRepository walletRepository) {
        this.createWalletService = createWalletService;
        this.updateWalletService = updateWalletService;
        this.depositFundsService = depositFundsService;
        this.walletRepository = walletRepository;
    }

    @PostMapping
    public ResponseEntity<WalletResponse> createWallet(
            @Valid @RequestBody CreateWalletCommand request,
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

        var wallets = walletRepository.findByUserId(userId);
        return ResponseEntity.ok(WalletMapper.toResponseList(wallets));
    }

    @GetMapping("/{walletId}")
    public ResponseEntity<WalletDetailResponse> getWallet(
            @PathVariable UUID walletId,
            @RequestHeader("X-User-Id") UUID userId) {

        var wallet = walletRepository.findById(WalletId.of(walletId))
                .orElseThrow(() -> new WalletNotFoundException(walletId));

        if (!wallet.getUserId().equals(userId)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }

        return ResponseEntity.ok(WalletMapper.toDetailResponse(wallet));
    }

    @PatchMapping("/{walletId}/balance")
    public ResponseEntity<WalletDetailResponse> adjustBalance(
            @PathVariable UUID walletId,
            @RequestHeader("X-User-Id") UUID userId,
            @RequestHeader(value = "X-Correlation-Id", required = false) String correlationId,
            @Valid @RequestBody AdjustBalanceCommand request) {

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
            @Valid @RequestBody DepositFundsCommand request) {

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
            @Valid @RequestBody UpdateStatusCommand request) {

        var result = updateWalletService.changeStatus(new UpdateWalletUseCase.StatusChangeCommand(
                walletId, userId, WalletStatus.valueOf(request.status().toUpperCase())));

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
