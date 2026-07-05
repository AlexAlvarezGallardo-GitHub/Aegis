package com.aegis.wallet.domain.model;

import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class WalletIdTest {

    @Test
    void generateShouldCreateUniqueId() {
        WalletId id1 = WalletId.generate();
        WalletId id2 = WalletId.generate();
        assertNotEquals(id1, id2);
    }

    @Test
    void ofShouldCreateFromUuid() {
        UUID uuid = UUID.randomUUID();
        WalletId id = WalletId.of(uuid);
        assertEquals(uuid, id.value());
    }

    @Test
    void ofShouldThrowOnNull() {
        assertThrows(NullPointerException.class, () -> WalletId.of(null));
    }

    @Test
    void toStringShouldReturnUuidString() {
        UUID uuid = UUID.randomUUID();
        WalletId id = WalletId.of(uuid);
        assertEquals(uuid.toString(), id.toString());
    }
}
