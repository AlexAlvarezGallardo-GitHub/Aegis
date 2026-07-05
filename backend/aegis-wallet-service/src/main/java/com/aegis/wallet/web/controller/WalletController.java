package com.aegis.wallet.web.controller;

import com.aegis.wallet.application.dto.CreateWalletCommand;
import com.aegis.wallet.application.dto.WalletResponse;
import com.aegis.wallet.application.mapper.WalletMapper;
import com.aegis.wallet.application.service.CreateWalletService;
import com.aegis.wallet.domain.exception.WalletNotFoundException;
import com.aegis.wallet.domain.model.WalletId;
import com.aegis.wallet.domain.port.outbound.WalletRepository;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
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
    private final WalletRepository walletRepository;

    public WalletController(CreateWalletService createWalletService,
                             WalletRepository walletRepository) {
        this.createWalletService = createWalletService;
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
    public ResponseEntity<WalletResponse> getWallet(
            @PathVariable UUID walletId,
            @RequestHeader("X-User-Id") UUID userId) {

        var wallet = walletRepository.findById(WalletId.of(walletId))
                .orElseThrow(() -> new WalletNotFoundException(walletId));

        if (!wallet.getUserId().equals(userId)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }

        return ResponseEntity.ok(WalletMapper.toResponse(wallet));
    }
}
