package com.app.infrastructure.storage.drivers;

import com.app.infrastructure.storage.StorageDriver;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.jboss.logging.Logger;
{{EXTRA_IMPORTS}}

import java.io.InputStream;

@ApplicationScoped
public class {{DRIVER_NAME}}Driver implements StorageDriver {

    private static final Logger LOG = Logger.getLogger({{DRIVER_NAME}}Driver.class);

    {{DRIVER_FIELDS}}

    @Override
    public void put(String path, byte[] content) {
        if (path == null || path.trim().isEmpty()) {
            throw new RuntimeException("Storage path cannot be empty");
        }
        try {
            {{PUT_BYTES_LOGIC}}
        } catch (Exception e) {
            LOG.errorf("[Storage][{{DRIVER_NAME}}] Failed to write file to %s: %s", path, e.getMessage());
            throw new RuntimeException("Failed to store file in {{DRIVER_NAME}}", e);
        }
    }

    @Override
    public void put(String path, InputStream content) {
        if (path == null || path.trim().isEmpty()) {
            throw new RuntimeException("Storage path cannot be empty");
        }
        try {
            {{PUT_STREAM_LOGIC}}
        } catch (Exception e) {
            LOG.errorf("[Storage][{{DRIVER_NAME}}] Failed to write file from stream to %s: %s", path, e.getMessage());
            throw new RuntimeException("Failed to store file in {{DRIVER_NAME}}", e);
        }
    }

    @Override
    public byte[] get(String path) {
        if (path == null || path.trim().isEmpty()) {
            throw new RuntimeException("Storage path cannot be empty");
        }
        try {
            {{GET_LOGIC}}
        } catch (Exception e) {
            LOG.errorf("[Storage][{{DRIVER_NAME}}] Failed to read file from %s: %s", path, e.getMessage());
            throw new RuntimeException("Failed to read file from {{DRIVER_NAME}}", e);
        }
    }

    @Override
    public boolean exists(String path) {
        try {
            {{EXISTS_LOGIC}}
        } catch (Exception e) {
            return false;
        }
    }

    @Override
    public void delete(String path) {
        if (path == null || path.trim().isEmpty()) {
            throw new RuntimeException("Storage path cannot be empty");
        }
        try {
            {{DELETE_LOGIC}}
        } catch (Exception e) {
            LOG.errorf("[Storage][{{DRIVER_NAME}}] Failed to delete file %s: %s", path, e.getMessage());
            throw new RuntimeException("Failed to delete file from {{DRIVER_NAME}}", e);
        }
    }

    @Override
    public String getUrl(String path) {
        {{GET_URL_LOGIC}}
    }

    @Override
    public String getName() {
        return "{{DRIVER_LOWER}}";
    }

    @Override
    public boolean checkHealth() {
        try {
            {{HEALTH_LOGIC}}
        } catch (Exception e) {
            LOG.error("[Storage][{{DRIVER_NAME}}] Health check failed: " + e.getMessage());
            return false;
        }
    }

    {{HELPER_METHODS}}
}
