package com.aegis.identity.application.service;

import com.aegis.identity.domain.model.TokenPair;
import com.aegis.identity.domain.model.UserId;
import com.aegis.identity.domain.port.inbound.RefreshTokenUseCase;
import com.aegis.identity.domain.port.outbound.TokenProvider;
import com.aegis.identity.domain.port.outbound.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class RefreshTokenService implements RefreshTokenUseCase {

    private final TokenProvider tokenProvider;
    private final UserRepository userRepository;

    public RefreshTokenService(TokenProvider tokenProvider, UserRepository userRepository) {
        this.tokenProvider = tokenProvider;
        this.userRepository = userRepository;
    }

    @Override
    @Transactional
    public Result refresh(Command command) {
        UserId userId = tokenProvider.validateRefreshToken(command.refreshToken());

        var user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found"));

        TokenPair tokenPair = tokenProvider.generateTokenPair(
                user.getUserId(),
                user.getEmail().value()
        );

        return new Result(tokenPair);
    }
}
