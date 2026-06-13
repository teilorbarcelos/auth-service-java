package com.app.infrastructure.storage.drivers;

import com.app.infrastructure.storage.StorageDriver;
import jakarta.enterprise.context.ApplicationScoped;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.jboss.logging.Logger;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;

@ApplicationScoped
public class LocalDriver implements StorageDriver {

    private static final Logger LOG = Logger.getLogger(LocalDriver.class);

    @ConfigProperty(name = "storage.local.path", defaultValue = "storage/app")
    String storagePath;

    @ConfigProperty(name = "storage.url", defaultValue = "http://localhost:8888/storage")
    String baseUrl;

    @Override
    public void put(String path, byte[] content) {
        if (path == null || path.trim().isEmpty()) {
            throw new RuntimeException("Storage path cannot be empty");
        }
        try {
            Path filePath = getFullPath(path);
            Files.createDirectories(filePath.getParent());
            Files.write(filePath, content, StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING);
        } catch (IOException e) {
            LOG.errorf("[Storage][Local] Failed to write file to %s: %s", path, e.getMessage());
            throw new RuntimeException("Failed to store file locally", e);
        }
    }

    @Override
    public void put(String path, InputStream content) {
        if (path == null || path.trim().isEmpty()) {
            throw new RuntimeException("Storage path cannot be empty");
        }
        try {
            Path filePath = getFullPath(path);
            Files.createDirectories(filePath.getParent());
            Files.copy(content, filePath, java.nio.file.StandardCopyOption.REPLACE_EXISTING);
        } catch (IOException e) {
            LOG.errorf("[Storage][Local] Failed to write file from stream to %s: %s", path, e.getMessage());
            throw new RuntimeException("Failed to store file locally", e);
        }
    }

    @Override
    public byte[] get(String path) {
        if (path == null || path.trim().isEmpty()) {
            throw new RuntimeException("Storage path cannot be empty");
        }
        try {
            return Files.readAllBytes(getFullPath(path));
        } catch (IOException e) {
            LOG.errorf("[Storage][Local] Failed to read file from %s: %s", path, e.getMessage());
            throw new RuntimeException("Failed to read file locally", e);
        }
    }

    @Override
    public boolean exists(String path) {
        return Files.exists(getFullPath(path));
    }

    @Override
    public void delete(String path) {
        try {
            Files.deleteIfExists(getFullPath(path));
        } catch (IOException e) {
            LOG.errorf("[Storage][Local] Failed to delete file %s: %s", path, e.getMessage());
            throw new RuntimeException("Failed to delete file locally", e);
        }
    }

    @Override
    public String getUrl(String path) {
        String cleanBaseUrl = baseUrl.endsWith("/") ? baseUrl.substring(0, baseUrl.length() - 1) : baseUrl;
        String cleanPath = path.startsWith("/") ? path : "/" + path;
        return cleanBaseUrl + cleanPath;
    }

    @Override
    public String getName() {
        return "local";
    }

    @Override
    public boolean checkHealth() {
        try {
            Path root = Paths.get(storagePath);
            if (!Files.exists(root)) {
                Files.createDirectories(root);
            }
            return Files.isWritable(root);
        } catch (Exception e) {
            LOG.error("[Storage][Local] Health check failed: " + e.getMessage());
            return false;
        }
    }

    private Path getFullPath(String path) {
        return Paths.get(storagePath, path);
    }
}
