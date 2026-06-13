package com.app.modules.product;

import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.Mockito.mock;

public class ProductServiceUnitTest {

    private ProductService productService;
    private ProductRepository productRepository;
    private EntityManager em;

    @BeforeEach
    void setup() {
        productRepository = mock(ProductRepository.class);
        em = mock(EntityManager.class);
        productService = new ProductService(productRepository, em);
    }

    @Test
    void testMergeFields_AllPopulated() {
        ProductModel existing = new ProductModel();
        ProductModel incoming = new ProductModel();
        incoming.setSku("SKU123");
        incoming.setName("Name123");
        incoming.setDescription("Desc123");
        incoming.setCategory("Cat123");
        incoming.setPrice(new BigDecimal("10.00"));
        incoming.setStock(50);
        incoming.setActive(true);

        productService.mergeFields(existing, incoming);

        assertEquals("SKU123", existing.getSku());
        assertEquals("Name123", existing.getName());
        assertEquals("Desc123", existing.getDescription());
        assertEquals("Cat123", existing.getCategory());
        assertEquals(new BigDecimal("10.00"), existing.getPrice());
        assertEquals(50, existing.getStock());
        assertEquals(true, existing.getActive());
    }

    @Test
    void testMergeFields_AllNull() {
        ProductModel existing = new ProductModel();
        existing.setName("OldName");
        ProductModel incoming = new ProductModel();
        incoming.setStock(null);
        incoming.setActive(null);

        productService.mergeFields(existing, incoming);

        assertEquals("OldName", existing.getName());
        assertNull(existing.getSku());
    }
}
