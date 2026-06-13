package com.app.infrastructure.storage;

import java.io.InputStream;

/**
 * Interface for storage drivers (Local, S3, GCS, Azure).
 */
public interface StorageDriver {
    /**
     * Stores content in the given path.
     */
    void put(String path, byte[] content);

    /**
     * Stores content from an InputStream in the given path.
     */
    void put(String path, InputStream content);

    /**
     * Retrieves content from the given path.
     */
    byte[] get(String path);

    /**
     * Checks if a file exists.
     */
    boolean exists(String path);

    /**
     * Deletes a file.
     */
    void delete(String path);

    /**
     * Gets a public URL for the file.
     */
    String getUrl(String path);

    /**
     * Gets the driver name.
     */
    String getName();

    /**
     * Checks driver health (e.g., connectivity).
     */
    boolean checkHealth();
}
