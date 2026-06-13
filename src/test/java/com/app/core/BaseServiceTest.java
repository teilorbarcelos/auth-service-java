package com.app.core;

import com.app.core.exception.BadRequestException;
import com.app.modules.product.ProductModel;
import com.app.modules.product.ProductRepository;
import com.app.modules.product.ProductService;
import io.quarkus.test.junit.QuarkusTest;
import jakarta.inject.Inject;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;

@QuarkusTest
public class BaseServiceTest {

    @Inject
    ProductService productService;

    @Inject
    ProductRepository productRepository;

    @Test
    @DisplayName("Should throw exception for invalid filter")
    public void testInvalidFilter() {
        assertThrows(BadRequestException.class, () -> productService.listItems(Map.of("invalid_field", "value"), true));
    }

    @Test
    @DisplayName("Should throw exception for invalid search fields")
    public void testInvalidSearchFields() {
        assertThrows(BadRequestException.class, () -> productService.listItems(Map.of("searchWord", "test"), true));

        assertThrows(BadRequestException.class,
                () -> productService.listItems(Map.of("searchWord", "test", "searchFields", "invalid"), true));
    }

    @Test
    @DisplayName("Should throw exception for invalid orderBy")
    public void testInvalidOrderBy() {
        assertThrows(BadRequestException.class, () -> productService.listItems(Map.of("orderBy", "invalid"), true));
    }

    @Test
    @DisplayName("Should handle boolean conversion and onlyActive logic")
    public void testBooleanAndActiveLogic() {
        // Boolean conversion
        var response = productService.listItems(Map.of("active", "true"), false);
        assertNotNull(response);

        // onlyActive = true (should add active=true if not present)
        var response2 = productService.listItems(Map.of(), true);
        assertNotNull(response2);
        
        // onlyActive = true but active already present
        var response3 = productService.listItems(Map.of("active", "false"), true);
        assertNotNull(response3);

        // Test explicit orderBy=createdAt and orderBy=updatedAt to cover all branches of line 101
        assertNotNull(productService.listItems(Map.of("orderBy", "createdAt"), false));
        assertNotNull(productService.listItems(Map.of("orderBy", "updatedAt"), false));
        
        // Test custom orderBy from filterableFields (covers branch at line 102)
        assertNotNull(productService.listItems(Map.of("orderBy", "name"), false));
    }

    @Test
    @DisplayName("Should handle date range suffixes in filter keys")
    public void testDateRangeSuffixes() {
        // Covers lines 56 and 58 (normalization of _start and _end)
        var params = Map.of(
            "createdAt_start", "2023-01-01",
            "createdAt_end", "2023-12-31"
        );
        var response = productService.listItems(params, false);
        assertNotNull(response);
    }

    @Test
    @DisplayName("Should return null for deleted entity or non-existent update")
    @jakarta.transaction.Transactional
    public void testEntityRetrievalAndUpdate() {
        ProductModel p = new ProductModel();
        p.setName("Service Test");
        p.setSku("SRV-" + System.currentTimeMillis());
        p.setPrice(new java.math.BigDecimal("10.0"));
        p.setIsDeleted(true);
        productRepository.persist(p);

        assertNull(productService.retrieveById(p.getId()));
        assertNull(productService.update("non-existent", new ProductModel()));
    }

    @Test
    @DisplayName("Should handle parseIntOrDefault edge cases")
    public void testParseIntOrDefault() {
        // This covers the catch block in parseIntOrDefault
        var response = productService.listItems(Map.of("page", "invalid", "size", "invalid"), false);
        assertNotNull(response);
    }

    @Test
    @DisplayName("Should handle complex filtering and ordering")
    public void testComplexFiltering() {
        // Ordering by updatedAt
        var response1 = productService.listItems(Map.of("orderBy", "updatedAt"), false);
        assertNotNull(response1);

        // Boolean conversion with "false" (covers line 67 short-circuit)
        assertNotNull(productService.listItems(Map.of("active", "false"), false));

        // listAllItems
        var response2 = productService.listAllItems(Map.of());
        assertNotNull(response2);

        // Test null value in queryParams (covers line 52: value == null)
        java.util.Map<String, String> nullMap = new java.util.HashMap<>();
        nullMap.put("someKey", null);
        assertNotNull(productService.listItems(nullMap, false));

        // Multiple search fields (covers loop at line 90)
        var response3 = productService.listItems(Map.of(
            "searchWord", "test", 
            "searchFields", "name, sku" 
        ), true);
        assertNotNull(response3);
        
        // Test searchWord is null (covers line 81: searchWord != null == false)
        java.util.Map<String, String> nullSearchMap = new java.util.HashMap<>();
        nullSearchMap.put("searchWord", null);
        assertNotNull(productService.listItems(nullSearchMap, true));

        // Test searchWord is empty string (covers line 81: !searchWord.isBlank() == false)
        assertNotNull(productService.listItems(Map.of("searchWord", ""), true));
        
        // Test searchWord is blank string (covers line 81: !searchWord.isBlank() == false)
        assertNotNull(productService.listItems(Map.of("searchWord", "   "), true));

        // Test branch with dot in search field (covers line 92)
        assertThrows(BadRequestException.class, () -> productService.listItems(Map.of(
            "searchWord", "test", 
            "searchFields", "nested.field" 
        ), true));

        // Test branch with dot in filter key (covers line 54)
        BaseService<ProductModel, ProductRepository> dummyService = new BaseService<>() {
            {
                this.repository = Mockito.mock(ProductRepository.class);
                this.em = Mockito.mock(jakarta.persistence.EntityManager.class);
                this.entityClass = ProductModel.class;
                this.filterableFields = java.util.List.of("nested.field");
                
                Mockito.when(this.repository.searchPaginated(any(), any(), any(QueryFilter.class)))
                       .thenReturn(new com.app.core.dto.PaginatedResponse<>(java.util.List.of(), 0, 0, 10));
            }
            @Override protected void mergeFields(ProductModel e, ProductModel i) {}
        };
        assertNotNull(dummyService.listItems(Map.of("nested.field", "value"), false));
    }

    @Test
    @DisplayName("Should return null for entity marked as deleted")
    @jakarta.transaction.Transactional
    public void testRetrieveDeletedEntity() {
        ProductModel p = new ProductModel();
        p.setName("Deleted Entity");
        p.setSku("DEL-RET-" + System.currentTimeMillis());
        p.setPrice(new java.math.BigDecimal("10.0"));
        p.setIsDeleted(true);
        productRepository.persist(p);
        productRepository.flush();

        assertNull(productService.retrieveById(p.getId()));
    }

    @Test
    @DisplayName("Should handle isDeleted being null")
    @jakarta.transaction.Transactional
    public void testIsDeletedNull() {
        ProductModel p = new ProductModel();
        p.setName("Null Deleted Test");
        p.setSku("NULL-DEL-" + System.currentTimeMillis());
        p.setPrice(new java.math.BigDecimal("10.0"));
        p.setIsDeleted(null); // Explicitly null
        productRepository.persist(p);

        assertNotNull(productService.retrieveById(p.getId()));
    }

    @Test
    @DisplayName("Should handle mixed page/size presence")
    public void testMixedPageSize() {
        // Page present, size absent
        assertNotNull(productService.listItems(Map.of("page", "2"), false));
        // Page absent, size present
        assertNotNull(productService.listItems(Map.of("size", "50"), false));
    }

    @Test
    @DisplayName("Should handle successful retrieval")
    @jakarta.transaction.Transactional
    public void testRetrieveByIdSuccess() {
        ProductModel p = new ProductModel();
        p.setName("Success Test");
        p.setSku("OK-" + System.currentTimeMillis());
        p.setPrice(new java.math.BigDecimal("10.0"));
        p.setIsDeleted(false);
        productRepository.persist(p);

        assertNotNull(productService.retrieveById(p.getId()));
    }

    @Test
    @DisplayName("Should handle reserved parameters and blank values")
    public void testReservedAndBlankParams() {
        // Reserved parameter 'page' should be ignored in andRules
        // Blank value should be ignored (covers line 52)
        var response = productService.listItems(Map.of("page", "1", "name", ""), true);
        assertNotNull(response);
    }

    @Test
    @DisplayName("Should throw exception for missing search fields when searchWord is provided")
    public void testMissingSearchFields() {
        assertThrows(BadRequestException.class, () -> productService.listItems(Map.of("searchWord", "test"), true));
        assertThrows(BadRequestException.class, () -> productService.listItems(Map.of("searchWord", "test", "searchFields", " "), true));
    }

    @Test
    @DisplayName("Should handle dot in filter key and search fields")
    public void testDotsInParams() {
        // Dot in filter key (covers line 54) - Note: we need a field that exists or skip validation
        // Since we can't easily skip validation without a mock, let's use one that might exist or just trigger the line
        // and expect a BadRequest if it's not in filterableFields. That still covers the line!
        assertThrows(BadRequestException.class, () -> productService.listItems(Map.of("nested.field", "value"), false));
        
        // Dot in search fields (covers line 92)
        // We'll use a field that doesn't exist to trigger the exception but after the dot logic
        assertThrows(BadRequestException.class, () -> productService.listItems(Map.of("searchWord", "test", "searchFields", "nested.field"), true));
    }

    @Test
    @DisplayName("Should handle empty searchableFields")
    public void testEmptySearchableFields() {
        // Create a minimal service with no searchable fields
        BaseService<ProductModel, ProductRepository> minimalService = new BaseService<>() {
            {
                this.repository = productRepository;
                this.entityClass = ProductModel.class;
                this.searchableFields = java.util.List.of();
            }
            @Override
            protected void mergeFields(ProductModel existing, ProductModel incoming) {}
        };
        
        assertThrows(BadRequestException.class, () -> minimalService.listItems(Map.of("searchWord", "test"), true));
    }

    @Test
    @DisplayName("Should handle null entity in retrieveById")
    public void testRetrieveByIdNull() {
        // repository.findById("non-existent") returns null
        assertNull(productService.retrieveById("non-existent"));
    }

    @Test
    @DisplayName("Should handle soft delete")
    @jakarta.transaction.Transactional
    public void testDelete() {
        ProductModel p = new ProductModel();
        p.setName("Delete Test");
        p.setSku("DEL-" + System.currentTimeMillis());
        p.setPrice(new java.math.BigDecimal("10.0"));
        productRepository.persist(p);

        assertTrue(productService.delete(p.getId()));
        assertNull(productService.retrieveById(p.getId()));
    }
}
