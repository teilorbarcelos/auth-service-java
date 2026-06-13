package com.app.infrastructure.storage.drivers;

import io.quarkus.test.junit.QuarkusTest;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Comparator;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Teste do LocalDriver dentro do contexto QuarkusTest para garantir a cobertura no jacoco-quarkus.exec.
 * Usamos instanciação manual em vez de @Inject para evitar problemas com Proxies CDI ao alterar campos.
 */
@QuarkusTest
class LocalDriverTest {

    private LocalDriver driver;
    private final String testFile = "test.txt";
    private final byte[] testContent = "Hello World".getBytes();
    private final String testDir = "target/storage-test-quarkus";

    @BeforeEach
    void setUp() throws IOException {
        driver = new LocalDriver();
        driver.storagePath = testDir;
        driver.baseUrl = "http://test-server:8080/storage";
        
        Path path = Paths.get(testDir);
        if (Files.exists(path)) {
            Files.walk(path)
                 .sorted(Comparator.reverseOrder())
                 .map(Path::toFile)
                 .forEach(java.io.File::delete);
        }
        Files.createDirectories(path);
    }

    @AfterEach
    void tearDown() throws IOException {
        Path path = Paths.get(testDir);
        if (Files.exists(path)) {
            Files.walk(path)
                 .sorted(Comparator.reverseOrder())
                 .map(Path::toFile)
                 .forEach(java.io.File::delete);
        }
    }

    @Test
    void testPutAndGet() {
        driver.put(testFile, testContent);
        assertTrue(driver.exists(testFile));
        assertArrayEquals(testContent, driver.get(testFile));
    }

    @Test
    void testPutInputStream() {
        driver.put(testFile, new ByteArrayInputStream(testContent));
        assertTrue(driver.exists(testFile));
        assertArrayEquals(testContent, driver.get(testFile));
    }

    @Test
    void testDelete() {
        driver.put(testFile, testContent);
        assertTrue(driver.exists(testFile));
        driver.delete(testFile);
        assertFalse(driver.exists(testFile));
        
        // Deletar arquivo que não existe (não deve lançar erro)
        driver.delete("not-found.txt");
        assertFalse(driver.exists("not-found.txt"));
    }

    @Test
    void testGetUrl() {
        // Teste sem barra no final
        String testUrl = "http://test-server:8080/storage";
        driver.baseUrl = testUrl;
        assertEquals(testUrl + "/test.txt", driver.getUrl("test.txt"));
        assertEquals(testUrl + "/test.txt", driver.getUrl("/test.txt"));
        
        driver.baseUrl = testUrl + "/";
        assertEquals(testUrl + "/test.txt", driver.getUrl("test.txt"));
    }

    @Test
    void testGetName() {
        assertEquals("local", driver.getName());
    }

    @Test
    void testCheckHealth() throws IOException {
        // Caso diretório exista e seja gravável
        assertTrue(driver.checkHealth());

        // Caso diretório NÃO exista (deve criar)
        Path nonExistent = Paths.get(testDir, "nested-health");
        driver.storagePath = nonExistent.toString();
        assertTrue(driver.checkHealth());
        assertTrue(Files.exists(nonExistent));

        // Caso de erro (diretório sem permissão de escrita)
        Path noWriteDir = Paths.get(testDir, "no-write");
        Files.createDirectories(noWriteDir);
        noWriteDir.toFile().setWritable(false);
        driver.storagePath = noWriteDir.toString();
        try {
            assertFalse(driver.checkHealth());
        } finally {
            noWriteDir.toFile().setWritable(true);
        }
    }

    @Test
    void testErrorHandling() {
        // Validation tests for put(byte[])
        assertThrows(RuntimeException.class, () -> driver.put("", new byte[0]));
        assertThrows(RuntimeException.class, () -> driver.put(null, new byte[0]));
        
        // Validation tests for put(InputStream)
        ByteArrayInputStream bais1 = new ByteArrayInputStream(new byte[0]);
        assertThrows(RuntimeException.class, () -> driver.put("", bais1));
        ByteArrayInputStream bais2 = new ByteArrayInputStream(new byte[0]);
        assertThrows(RuntimeException.class, () -> driver.put(null, bais2));

        // Validation tests for get()
        assertThrows(RuntimeException.class, () -> driver.get(""));
        assertThrows(RuntimeException.class, () -> driver.get(null));
        assertThrows(RuntimeException.class, () -> driver.get("non-existent.txt"));
        
        // IOException tests
        // Point storage to a file so createDirectories or writing fails
        Path filePath = Paths.get(testDir, "a-file");
        try {
            Files.write(filePath, "content".getBytes());
            driver.storagePath = filePath.toString();
            
            // This should fail because it tries to create directories under a file
            assertThrows(RuntimeException.class, () -> driver.put("some/path", new byte[0]));
            ByteArrayInputStream bais3 = new ByteArrayInputStream(new byte[0]);
            assertThrows(RuntimeException.class, () -> driver.put("some/path", bais3));
            
            // get() on a directory (testDir) might throw IOException on some systems or just return bytes if it's a file
            // Let's use a path that is definitely problematic
            driver.storagePath = testDir;
            assertThrows(RuntimeException.class, () -> driver.get("")); // Empty path with storagePath might point to directory
            
        } catch (IOException e) {
            fail("Setup failed: " + e.getMessage());
        }

        // Simular erro de IO no delete
        // Deletar um diretório não vazio ou algo que cause IOException
        assertThrows(RuntimeException.class, () -> driver.delete("."));
    }

    @Test
    void testCheckHealthException() {
        // Trigger Exception in checkHealth
        driver.storagePath = null;
        assertFalse(driver.checkHealth());
    }
}
