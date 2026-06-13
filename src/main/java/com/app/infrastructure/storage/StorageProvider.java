package com.app.infrastructure.storage;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.inject.Instance;
import jakarta.inject.Inject;
import org.eclipse.microprofile.config.inject.ConfigProperty;

import java.io.InputStream;

@ApplicationScoped
public class StorageProvider {

    @ConfigProperty(name = "storage.disk", defaultValue = "local")
    String activeDisk;

    @Inject
    Instance<StorageDriver> drivers;

    private StorageDriver driver;

    public StorageDriver getDriver() {
        if (driver == null) {
            driver = drivers.stream()
                    .filter(d -> d.getName().equalsIgnoreCase(activeDisk))
                    .findFirst()
                    .orElseThrow(() -> new RuntimeException("Storage driver [" + activeDisk + "] not found."));
        }
        return driver;
    }

    public void put(String path, byte[] content) {
        getDriver().put(path, content);
    }

    public void put(String path, InputStream content) {
        getDriver().put(path, content);
    }

    public byte[] get(String path) {
        return getDriver().get(path);
    }

    public boolean exists(String path) {
        return getDriver().exists(path);
    }

    public void delete(String path) {
        getDriver().delete(path);
    }

    public String getUrl(String path) {
        return getDriver().getUrl(path);
    }

    public String getActiveDisk() {
        return activeDisk;
    }

    public boolean checkHealth() {
        try {
            return getDriver().checkHealth();
        } catch (Exception e) {
            return false;
        }
    }
}
