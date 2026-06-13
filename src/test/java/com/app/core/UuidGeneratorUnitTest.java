package com.app.core;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class UuidGeneratorUnitTest {

    @Test
    void testGenerate() {
        String uuid = UuidGenerator.generate();
        assertNotNull(uuid);
        assertFalse(uuid.isBlank());

        String uuid2 = UuidGenerator.generate();
        assertNotNull(uuid2);
        assertNotEquals(uuid, uuid2);
    }
}
