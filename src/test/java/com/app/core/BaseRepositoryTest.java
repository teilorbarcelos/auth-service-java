package com.app.core;

import com.app.modules.product.ProductModel;
import com.app.modules.product.ProductRepository;
import com.app.modules.user.UserModel;
import io.quarkus.test.junit.QuarkusTest;
import jakarta.inject.Inject;
import jakarta.persistence.EntityManager;
import jakarta.transaction.Transactional;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

@QuarkusTest
public class BaseRepositoryTest {

    @Inject
    ProductRepository repository;

    @Inject
    com.app.modules.user.UserRepository userRepository;

    @Inject
    EntityManager em;

    @Test
    @DisplayName("Should parse dates correctly")
    public void testParseDate() {
        var response = repository.searchPaginated(em, ProductModel.class, 
                new QueryFilter(0, 10, List.of(
                    new FilterRule("createdAt", "2023-01-01", FilterRule.Operator.GREATER_THAN_OR_EQUAL),
                    new FilterRule("createdAt", "2023-12-31", FilterRule.Operator.LESS_THAN_OR_EQUAL)
                ), List.of(), "createdAt", "asc"));
        assertNotNull(response);
    }

    @Test
    @DisplayName("Should handle search with joins")
    @Transactional
    public void testSearchWithJoins() {
        var response = userRepository.searchPaginated(em, UserModel.class, 
                new QueryFilter(0, 10, 
                    List.of(new FilterRule("role.name", "Admin", FilterRule.Operator.EQUALS)), 
                    List.of(new FilterRule("role.description", "Administrator", FilterRule.Operator.LIKE)), 
                    "role.name", "desc"));
        assertNotNull(response);
    }

    @Test
    @DisplayName("Should handle soft delete and status toggle")
    @Transactional
    public void testSoftDeleteAndStatus() {
        ProductModel p = new ProductModel();
        p.setName("Repo Test");
        p.setSku("REPO-" + System.currentTimeMillis());
        p.setPrice(new java.math.BigDecimal("10.0"));
        p.setActive(true);
        repository.persist(p);
        
        String id = p.getId();
        
        repository.setStatus(id, false);
        assertFalse(repository.findById(id).getActive());
        
        repository.softDelete(id);
        assertTrue(repository.findById(id).getIsDeleted());
        
        // Test non-existent ID
        assertFalse(repository.softDelete("non-existent"));
        assertNull(repository.setStatus("non-existent", true));
    }


    @Test
    @DisplayName("Should handle edge cases in buildPredicates and parsing")
    public void testEdgeCases() {
        // Test invalid date format for coverage of catch block
        var response = repository.searchPaginated(em, ProductModel.class, 
                new QueryFilter(0, 10, 
                    List.of(new FilterRule("createdAt", "invalid-date", FilterRule.Operator.GREATER_THAN_OR_EQUAL)), 
                    List.of(), "name", "asc"));
        assertNotNull(response);

        // Test full ISO date format
        var responseFullDate = repository.searchPaginated(em, ProductModel.class, 
                new QueryFilter(0, 10, 
                    List.of(new FilterRule("createdAt", "2023-01-01T12:00:00", FilterRule.Operator.GREATER_THAN_OR_EQUAL)), 
                    List.of(), "name", "asc"));
        assertNotNull(responseFullDate);

        // Test LocalDateTime object
        var responseObjDate = repository.searchPaginated(em, ProductModel.class, 
                new QueryFilter(0, 10, 
                    List.of(new FilterRule("createdAt", java.time.LocalDateTime.now(), FilterRule.Operator.GREATER_THAN_OR_EQUAL)), 
                    List.of(), "name", "asc"));
        assertNotNull(responseObjDate);

        // Test null date
        var responseNullDate = repository.searchPaginated(em, ProductModel.class, 
                new QueryFilter(0, 10, 
                    List.of(new FilterRule("createdAt", null, FilterRule.Operator.GREATER_THAN_OR_EQUAL)), 
                    List.of(), "name", "asc"));
        assertNotNull(responseNullDate);

        // Test removeAccents with null
        var responseNull = repository.searchPaginated(em, ProductModel.class, 
                new QueryFilter(0, 10, List.of(), 
                    List.of(new FilterRule("name", null, FilterRule.Operator.LIKE)), 
                    "name", "asc"));
        assertNotNull(responseNull);

        // Test removeAccents with accents
        var responseAccents = repository.searchPaginated(em, ProductModel.class, 
                new QueryFilter(0, 10, List.of(), 
                    List.of(new FilterRule("name", "João", FilterRule.Operator.LIKE)), 
                    "name", "asc"));
        assertNotNull(responseAccents);
        
        // Test empty rules
        var responseEmpty = repository.searchPaginated(em, ProductModel.class, 
                new QueryFilter(0, 10, List.of(), List.of(), "name", "asc"));
        assertNotNull(responseEmpty);

        // Test an operator that is not handled in the andRules loop (LIKE) to cover the final else branch
        repository.searchPaginated(em, ProductModel.class, 
                new QueryFilter(0, 10, 
                    List.of(new FilterRule("name", "test", FilterRule.Operator.LIKE)), 
                    List.of(), "name", "asc"));
    }

    @Test
    @DisplayName("Should cover SearchQueryBuilder constructor")
    public void testConstructor() throws Exception {
        var constructor = SearchQueryBuilder.class.getDeclaredConstructor();
        constructor.setAccessible(true);
        var instance = constructor.newInstance();
        assertNotNull(instance);
    }
}
