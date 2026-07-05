package com.aegis.bff;

import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.Base64;
import java.util.Map;
import java.util.UUID;

@Profile("dev")
@Service
public class MockLoginService {

    private static final String MOCK_EMAIL = "mock@aegis.dev";
    private static final UUID MOCK_USER_ID = UUID.fromString("00000000-0000-0000-0000-000000000001");

    private static final String HEADER = Base64.getUrlEncoder().withoutPadding()
            .encodeToString("{\"alg\":\"HS256\",\"typ\":\"JWT\"}".getBytes());

    private final SessionJwtStore sessionJwtStore;

    public MockLoginService(SessionJwtStore sessionJwtStore) {
        this.sessionJwtStore = sessionJwtStore;
    }

    public Map<String, Object> mockLogin() {
        long now = Instant.now().getEpochSecond();
        String payloadJson = String.format(
                "{\"sub\":\"%s\",\"email\":\"%s\",\"type\":\"access\",\"iat\":%d,\"exp\":%d}",
                MOCK_USER_ID, MOCK_EMAIL, now, now + 86400
        );
        String payload = Base64.getUrlEncoder().withoutPadding()
                .encodeToString(payloadJson.getBytes());
        String signature = Base64.getUrlEncoder().withoutPadding()
                .encodeToString("mock-signature".getBytes());

        String accessToken = HEADER + "." + payload + "." + signature;
        String refreshToken = HEADER + "." + payload + "." + signature;

        sessionJwtStore.storeTokens(accessToken, refreshToken);

        return Map.of(
                "tokenType", "Bearer",
                "expiresIn", 86400L,
                "emailVerified", true,
                "mock", true
        );
    }

    public static UUID getMockUserId() {
        return MOCK_USER_ID;
    }
}
