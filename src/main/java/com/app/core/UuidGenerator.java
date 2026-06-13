package com.app.core;

import java.security.SecureRandom;
import java.util.UUID;

public final class UuidGenerator {

    private static final SecureRandom RANDOM = new SecureRandom();

    private UuidGenerator() {
    }

    /**
     * Generate a time-ordered UUID v7 string (RFC 9562).
     * Reduces B-tree index fragmentation compared to UUID v4.
     */
    public static String generate() {
        long timestamp = System.currentTimeMillis();

        long mostSigBits = timestamp << 16;
        mostSigBits |= RANDOM.nextInt() & 0xFFFF;
        mostSigBits |= 0x7000_0000_0000_0000L;

        long leastSigBits = 0x8000_0000_0000_0000L;
        leastSigBits |= (RANDOM.nextLong() & 0x3FFF_FFFF_FFFF_FFFFL);

        return new UUID(mostSigBits, leastSigBits).toString();
    }
}
