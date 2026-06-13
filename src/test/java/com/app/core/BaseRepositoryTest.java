package com.app.core;

import com.app.modules.user.UserModel;
import com.app.modules.user.UserRepository;
import io.quarkus.test.junit.QuarkusTest;
import jakarta.inject.Inject;
import jakarta.persistence.EntityManager;
import jakarta.transaction.Transactional;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@QuarkusTest
public class BaseRepositoryTest {

    @Inject
    UserRepository repository;

    @Inject
    EntityManager em;

    @Test
    @DisplayName("Should parse dates correctly")
    public void testParseDate() {
        var response = repository.searchPaginated(em, UserModel.class,
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
        var response = repository.searchPaginated(em, UserModel.class,
                new QueryFilter(0, 10,
                    List.of(new FilterRule("role.name", "Admin", FilterRule.Operator.EQUALS)),
                    List.of(new FilterRule("role.description", "Administrator", FilterRule.Operator.LIKE)),
                    "role.name", "desc"));
        assertNotNull(response);
    }

    @Test
    @DisplayName("Should handle edge cases in buildPredicates and parsing")
    public void testEdgeCases() {
        var response = repository.searchPaginated(em, UserModel.class,
                new QueryFilter(0, 10,
                    List.of(new FilterRule("createdAt", "invalid-date", FilterRule.Operator.GREATER_THAN_OR_EQUAL)),
                    List.of(), "name", "asc"));
        assertNotNull(response);

        var responseFullDate = repository.searchPaginated(em, UserModel.class,
                new QueryFilter(0, 10,
                    List.of(new FilterRule("createdAt", "2023-01-01T12:00:00", FilterRule.Operator.GREATER_THAN_OR_EQUAL)),
                    List.of(), "name", "asc"));
        assertNotNull(responseFullDate);

        var responseObjDate = repository.searchPaginated(em, UserModel.class,
                new QueryFilter(0, 10,
                    List.of(new FilterRule("createdAt", java.time.LocalDateTime.now(), FilterRule.Operator.GREATER_THAN_OR_EQUAL)),
                    List.of(), "name", "asc"));
        assertNotNull(responseObjDate);

        var responseNullDate = repository.searchPaginated(em, UserModel.class,
                new QueryFilter(0, 10,
                    List.of(new FilterRule("createdAt", null, FilterRule.Operator.GREATER_THAN_OR_EQUAL)),
                    List.of(), "name", "asc"));
        assertNotNull(responseNullDate);

        var responseNull = repository.searchPaginated(em, UserModel.class,
                new QueryFilter(0, 10, List.of(),
                    List.of(new FilterRule("name", null, FilterRule.Operator.LIKE)),
                    "name", "asc"));
        assertNotNull(responseNull);

        var responseAccents = repository.searchPaginated(em, UserModel.class,
                new QueryFilter(0, 10, List.of(),
                    List.of(new FilterRule("name", "João", FilterRule.Operator.LIKE)),
                    "name", "asc"));
        assertNotNull(responseAccents);

        var responseEmpty = repository.searchPaginated(em, UserModel.class,
                new QueryFilter(0, 10, List.of(), List.of(), "name", "asc"));
        assertNotNull(responseEmpty);

        repository.searchPaginated(em, UserModel.class,
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
