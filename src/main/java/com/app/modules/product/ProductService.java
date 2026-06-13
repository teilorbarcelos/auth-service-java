package com.app.modules.product;

import com.app.core.BaseService;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.persistence.EntityManager;

import java.util.List;

/**
 * Product service.
 * Equivalent to ProductService.php
 */
@ApplicationScoped
public class ProductService extends BaseService<ProductModel, ProductRepository> {

    @Inject
    public ProductService(ProductRepository productRepository, EntityManager em) {
        this.repository = productRepository;
        this.em = em;
        this.entityClass = ProductModel.class;
        
        this.filterableFields = List.of("name", "active", "sku", "createdAt");
        this.searchableFields = List.of("name", "sku", "description");
    }

    @Override
    protected void mergeFields(ProductModel existing, ProductModel incoming) {
        if (incoming.getSku() != null)
            existing.setSku(incoming.getSku());
        if (incoming.getName() != null)
            existing.setName(incoming.getName());
        if (incoming.getDescription() != null)
            existing.setDescription(incoming.getDescription());
        if (incoming.getCategory() != null)
            existing.setCategory(incoming.getCategory());
        if (incoming.getPrice() != null)
            existing.setPrice(incoming.getPrice());
        if (incoming.getStock() != null)
            existing.setStock(incoming.getStock());
        if (incoming.getActive() != null)
            existing.setActive(incoming.getActive());
    }
}
