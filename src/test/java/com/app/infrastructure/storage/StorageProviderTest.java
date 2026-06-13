package com.app.infrastructure.storage;

import io.quarkus.test.junit.QuarkusTest;
import jakarta.inject.Inject;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;

import static org.junit.jupiter.api.Assertions.*;

@QuarkusTest
public class StorageProviderTest {

    @Inject
    StorageProvider storageProvider;

    @Test
    void testActiveDisk() {
        assertEquals("local", storageProvider.getActiveDisk());
        assertNotNull(storageProvider.getDriver());
        assertEquals("local", storageProvider.getDriver().getName());
    }

    @Test
    void testOperations() {
        String path = "provider-test.txt";
        byte[] content = "test content".getBytes();

        storageProvider.put(path, content);
        assertTrue(storageProvider.exists(path));
        assertArrayEquals(content, storageProvider.get(path));

        storageProvider.delete(path);
        assertFalse(storageProvider.exists(path));
    }

    @Test
    void testPutInputStream() {
        String path = "provider-stream-test.txt";
        byte[] content = "stream content".getBytes();

        storageProvider.put(path, new ByteArrayInputStream(content));
        assertTrue(storageProvider.exists(path));
        assertArrayEquals(content, storageProvider.get(path));

        storageProvider.delete(path);
    }

    @Test
    void testGetUrl() {
        String path = "url-test.txt";
        String url = storageProvider.getUrl(path);
        assertNotNull(url);
        assertTrue(url.contains(path));
    }

    @Test
    void testCheckHealth() {
        assertTrue(storageProvider.checkHealth());
    }
}
