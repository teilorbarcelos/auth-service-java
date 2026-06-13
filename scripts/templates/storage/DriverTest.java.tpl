package com.app.infrastructure.storage.drivers;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
{{EXTRA_IMPORTS}}

import java.io.ByteArrayInputStream;
import java.io.InputStream;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;

public class {{DRIVER_NAME}}DriverTest {

    private {{DRIVER_NAME}}Driver driver;
    private {{CLIENT_CLASS}} client;

    @BeforeEach
    void setUp() {
        client = Mockito.mock({{CLIENT_CLASS}}.class);
        driver = new {{DRIVER_NAME}}Driver();
        {{SETUP_LOGIC}}
    }

    @Test
    void testPutAndGet() {
        {{MOCK_PUT_GET}}
        
        String path = "test.txt";
        byte[] content = "hello".getBytes();
        
        driver.put(path, content);
        assertTrue(driver.exists(path));
        assertArrayEquals(content, driver.get(path));
    }

    @Test
    void testPutInputStream() {
        {{MOCK_PUT_STREAM}}
        
        String path = "test-stream.txt";
        byte[] content = "hello stream".getBytes();
        
        driver.put(path, new ByteArrayInputStream(content));
        assertTrue(driver.exists(path));
    }

    @Test
    void testExistsFailure() {
        {{MOCK_EXISTS_FAILURE}}
        assertFalse(driver.exists("non-existent.txt"));
    }

    @Test
    void testDelete() {
        {{MOCK_DELETE}}
        
        String path = "delete-me.txt";
        driver.delete(path);
    }

    @Test
    void testGetUrl() {
        String path = "url.txt";
        String url = driver.getUrl(path);
        assertNotNull(url);
    }

    @Test
    void testGetName() {
        assertEquals("{{DRIVER_LOWER}}", driver.getName());
    }

    @Test
    void testCheckHealth() {
        {{MOCK_HEALTH}}
        assertTrue(driver.checkHealth());
        
        {{MOCK_HEALTH_FAILURE}}
        assertFalse(driver.checkHealth());
    }

    @Test
    void testPutFailure() {
        assertThrows(RuntimeException.class, () -> driver.put("", "data".getBytes()));
        assertThrows(RuntimeException.class, () -> driver.put(null, "data".getBytes()));
        assertThrows(RuntimeException.class, () -> driver.put("   ", "data".getBytes()));

        {{MOCK_PUT_FAILURE}}
        assertThrows(RuntimeException.class, () -> driver.put("error.txt", "data".getBytes()));
    }

    @Test
    void testPutStreamFailure() {
        assertThrows(RuntimeException.class, () -> driver.put("", new ByteArrayInputStream("data".getBytes())));
        assertThrows(RuntimeException.class, () -> driver.put(null, new ByteArrayInputStream("data".getBytes())));
        assertThrows(RuntimeException.class, () -> driver.put("   ", new ByteArrayInputStream("data".getBytes())));
        
        {{EXTRA_STREAM_TEST}}

        {{MOCK_PUT_FAILURE}}
        assertThrows(RuntimeException.class, () -> driver.put("error.txt", new ByteArrayInputStream("data".getBytes())));
    }

    @Test
    void testGetFailure() {
        assertThrows(RuntimeException.class, () -> driver.get(""));
        assertThrows(RuntimeException.class, () -> driver.get(null));
        assertThrows(RuntimeException.class, () -> driver.get("   "));

        {{MOCK_GET_FAILURE}}
        assertThrows(RuntimeException.class, () -> driver.get("error.txt"));
    }

    @Test
    void testDeleteFailure() {
        assertThrows(RuntimeException.class, () -> driver.delete(""));
        assertThrows(RuntimeException.class, () -> driver.delete(null));
        assertThrows(RuntimeException.class, () -> driver.delete("   "));

        {{MOCK_DELETE_FAILURE}}
        assertThrows(RuntimeException.class, () -> driver.delete("error.txt"));
    }
}
