package com.aegis.common.util;

import java.security.SecureRandom;
import java.util.UUID;

/**
 * Generates UUIDv7 values: time-ordered, database-friendly unique identifiers.
 */
public final class UuidV7Generator {

    private static final SecureRandom RANDOM = new SecureRandom();

    private UuidV7Generator() {
    }

    /**
     * Generates a new UUIDv7 based on the current Unix timestamp and random bytes.
     *
     * @return a new UUIDv7 instance
     */
    public static UUID generate() {
        long timestamp = System.currentTimeMillis();
        byte[] randomBytes = new byte[10];
        RANDOM.nextBytes(randomBytes);

        long msb = (timestamp << 16)
                | (0x7L << 12)
                | ((randomBytes[0] & 0x0FL) << 8)
                | (randomBytes[1] & 0xFFL);

        long lsb = ((randomBytes[2] & 0x3FL) | 0x80L) << 56;
        for (int i = 3; i < 10; i++) {
            lsb |= (randomBytes[i] & 0xFFL) << ((9 - i) * 8);
        }

        return new UUID(msb, lsb);
    }
}
