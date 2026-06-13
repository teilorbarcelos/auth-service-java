package com.app.core;

import com.app.core.dto.PaginatedResponse;
import jakarta.persistence.EntityManager;
import jakarta.transaction.Transactional;
import java.util.*;

/**
 * Base service with CRUD, advanced filtering, and validation.
 * Equivalent to BaseService.php with filterableFields/searchableFields.
 */
public abstract class BaseService<E extends BaseEntity, R extends BaseRepository<E>> {

    protected R repository;
    protected EntityManager em;
    protected Class<E> entityClass;

    /**
     * Fields that can be used as exact-match filters in query params.
     * Override in subclasses.
     */
    protected List<String> filterableFields = List.of();

    /**
     * Fields that can be searched with ILIKE via searchWord.
     * Override in subclasses.
     */
    protected List<String> searchableFields = List.of();

    /**
     * Paginated listing with advanced filtering.
     * Exact port of BaseService.php::listItems.
     */
    public PaginatedResponse<E> listItems(Map<String, String> queryParams, boolean onlyActive) {
        QueryFilter filter = QueryFilter.fromQueryParams(queryParams, filterableFields, searchableFields, onlyActive);
        return repository.searchPaginated(em, entityClass, filter);
    }

    public PaginatedResponse<E> listAllItems(Map<String, String> queryParams) {
        return listItems(queryParams, false);
    }

    public E retrieveById(String id) {
        E entity = repository.findById(id);
        if (entity != null && Boolean.TRUE.equals(entity.getIsDeleted())) {
            return null;
        }
        return entity;
    }

    @Transactional
    public E create(E entity) {
        repository.persist(entity);
        return entity;
    }

    @Transactional
    public E update(String id, E updatedData) {
        E existing = repository.findById(id);
        if (existing == null)
            return null;
        mergeFields(existing, updatedData);
        repository.persist(existing);
        return existing;
    }

    @Transactional
    public boolean delete(String id) {
        return repository.softDelete(id);
    }

    @Transactional
    public E setStatus(String id, boolean active) {
        return repository.setStatus(id, active);
    }

    /**
     * Override this to define how fields are merged during update.
     */
    protected abstract void mergeFields(E existing, E incoming);

}
