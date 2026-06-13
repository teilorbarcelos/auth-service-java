package com.app.core;

import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.*;

class BaseEntityUnitTest {

    @Test
    void testBaseEntityGettersSetters() {
        BaseEntity entity = new BaseEntity() {};

        entity.setId("test-id");
        entity.setActive(false);
        entity.setIsDeleted(true);
        LocalDateTime deletedAt = LocalDateTime.of(2026, 6, 1, 10, 0);
        entity.setDeletedAt(deletedAt);
        LocalDateTime createdAt = LocalDateTime.of(2026, 1, 1, 0, 0);
        entity.setCreatedAt(createdAt);
        LocalDateTime updatedAt = LocalDateTime.of(2026, 6, 13, 12, 0);
        entity.setUpdatedAt(updatedAt);

        assertEquals("test-id", entity.getId());
        assertFalse(entity.getActive());
        assertTrue(entity.getIsDeleted());
        assertEquals(deletedAt, entity.getDeletedAt());
        assertEquals(createdAt, entity.getCreatedAt());
        assertEquals(updatedAt, entity.getUpdatedAt());
    }

    @Test
    void testBaseEntityDefaults() {
        BaseEntity entity = new BaseEntity() {};
        assertNull(entity.getId());
        assertTrue(entity.getActive());
        assertFalse(entity.getIsDeleted());
        assertNull(entity.getDeletedAt());
        assertNull(entity.getCreatedAt());
        assertNull(entity.getUpdatedAt());
    }

    @Test
    void testBaseEntityOnCreate() {
        BaseEntity entity = new BaseEntity() {};
        entity.onCreate();

        assertNotNull(entity.getId());
        assertNotNull(entity.getCreatedAt());
        assertNotNull(entity.getUpdatedAt());
    }

    @Test
    void testBaseEntityOnCreateWithExistingId() {
        BaseEntity entity = new BaseEntity() {};
        entity.setId("pre-set-id");
        LocalDateTime before = LocalDateTime.now();
        entity.onCreate();

        assertEquals("pre-set-id", entity.getId());
        assertNotNull(entity.getCreatedAt());
        assertNotNull(entity.getUpdatedAt());
        assertTrue(!entity.getCreatedAt().isBefore(before) || entity.getCreatedAt().equals(before));
    }

    @Test
    void testBaseEntityOnUpdate() {
        BaseEntity entity = new BaseEntity() {};
        entity.onCreate();
        LocalDateTime originalCreated = entity.getCreatedAt();
        LocalDateTime originalUpdated = entity.getUpdatedAt();

        entity.onUpdate();
        assertTrue(entity.getUpdatedAt().isAfter(originalUpdated) || entity.getUpdatedAt().equals(originalUpdated));
        assertEquals(originalCreated, entity.getCreatedAt());
    }
}
