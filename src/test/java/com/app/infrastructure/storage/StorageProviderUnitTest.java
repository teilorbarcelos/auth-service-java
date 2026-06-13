package com.app.infrastructure.storage;

import jakarta.enterprise.inject.Instance;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.when;

public class StorageProviderUnitTest {

    private StorageProvider provider;
    private Instance<StorageDriver> drivers;

    @BeforeEach
    void setUp() {
        provider = new StorageProvider();
        drivers = Mockito.mock(Instance.class);
        provider.drivers = drivers;
    }

    @Test
    void testDriverNotFound() {
        provider.activeDisk = "invalid";
        when(drivers.stream()).thenReturn(Stream.empty());

        RuntimeException exception = assertThrows(RuntimeException.class, () -> provider.getDriver());
        assertTrue(exception.getMessage().contains("Storage driver [invalid] not found."));
    }

    @Test
    void testCheckHealthException() {
        provider.activeDisk = "local";
        StorageDriver mockDriver = Mockito.mock(StorageDriver.class);
        
        when(mockDriver.getName()).thenReturn("local");
        when(mockDriver.checkHealth()).thenThrow(new RuntimeException("Health check failed"));
        when(drivers.stream()).thenReturn(Stream.of(mockDriver));

        assertFalse(provider.checkHealth());
    }
}
