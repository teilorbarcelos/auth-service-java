package com.app.core;

import com.app.core.dto.PaginatedResponse;
import io.quarkus.hibernate.orm.panache.PanacheRepositoryBase;
import jakarta.persistence.EntityManager;
import java.time.LocalDateTime;

/**
 * Base repository with advanced search, pagination, and soft delete.
 */
public abstract class BaseRepository<E extends BaseEntity> implements PanacheRepositoryBase<E, String> {
    
    /**
     * Advanced paginated search with AND filters, OR search, and ordering.
     * Delegates to SearchQueryBuilder for Criteria API construction.
     */
    public PaginatedResponse<E> searchPaginated(
            EntityManager em,
            Class<E> entityClass,
            QueryFilter filter) {
        return SearchQueryBuilder.buildAndExecute(em, entityClass, filter);
    }

    /**
     * Advanced search without pagination.
     */
    public java.util.List<E> searchAll(
            EntityManager em,
            Class<E> entityClass,
            QueryFilter filter) {
        return SearchQueryBuilder.buildAndExecuteAll(em, entityClass, filter);
    }

    /**
     * Soft delete: marks record as deleted and inactive.
     */
    public boolean softDelete(String id) {
        E entity = findById(id);
        if (entity == null)
            return false;

        entity.setActive(false);
        entity.setIsDeleted(true);
        entity.setDeletedAt(LocalDateTime.now());
        persist(entity);
        return true;
    }

    /**
     * Toggle active status.
     */
    public E setStatus(String id, boolean active) {
        E entity = findById(id);
        if (entity == null)
            return null;

        entity.setActive(active);
        persist(entity);
        return entity;
    }
}
