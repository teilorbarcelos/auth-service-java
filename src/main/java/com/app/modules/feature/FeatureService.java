package com.app.modules.feature;

import com.app.core.BaseService;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.persistence.EntityManager;
import java.util.List;

/**
 * Feature service — read-only operations.
 * Equivalent to FeatureService.php
 */
@ApplicationScoped
public class FeatureService extends BaseService<FeatureModel, FeatureRepository> {

    @Inject
    public FeatureService(FeatureRepository featureRepository, EntityManager em) {
        this.repository = featureRepository;
        this.em = em;
        this.entityClass = FeatureModel.class;
        this.filterableFields = List.of("name", "active", "createdAt");
        this.searchableFields = List.of("name", "description");
    }

    @Override
    protected void mergeFields(FeatureModel existing, FeatureModel incoming) {
        if (incoming.getName() != null)
            existing.setName(incoming.getName());
        if (incoming.getDescription() != null)
            existing.setDescription(incoming.getDescription());
        if (incoming.getActive() != null)
            existing.setActive(incoming.getActive());
    }
}
