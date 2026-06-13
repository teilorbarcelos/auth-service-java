package com.app.core;

import com.app.core.exception.BadRequestException;
import org.junit.jupiter.api.Test;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;

public class QueryFilterUnitTest {

    @Test
    void testQueryFilter_Normal() {
        Map<String, String> params = new HashMap<>();
        params.put("page", "2");
        params.put("size", "20");
        params.put("orderBy", "createdAt");
        params.put("orderDirection", "asc");

        QueryFilter filter = QueryFilter.fromQueryParams(params, List.of("name"), List.of(), false);
        assertEquals(2, filter.getPage());
        assertEquals(20, filter.getSize());
        assertEquals("createdAt", filter.getOrderBy());
        assertEquals("asc", filter.getOrderDirection());
        assertTrue(filter.getAndRules().isEmpty());
        assertTrue(filter.getOrRules().isEmpty());
    }

    @Test
    void testQueryFilter_SizeTooLarge() {
        Map<String, String> params = Map.of("size", "101");
        assertThrows(BadRequestException.class, () -> 
            QueryFilter.fromQueryParams(params, List.of(), List.of(), false)
        );
    }

    @Test
    void testQueryFilter_ParseIntOrDefault_Exception() {
        Map<String, String> params = Map.of("page", "abc", "size", "xyz");
        QueryFilter filter = QueryFilter.fromQueryParams(params, List.of(), List.of(), false);
        assertEquals(0, filter.getPage());
        assertEquals(10, filter.getSize());
    }

    @Test
    void testQueryFilter_InvalidStartDate() {
        Map<String, String> params = Map.of("createdAt_start", "invalid-date");
        assertThrows(BadRequestException.class, () -> 
            QueryFilter.fromQueryParams(params, List.of("createdat"), List.of(), false)
        );
    }

    @Test
    void testQueryFilter_InvalidEndDate() {
        Map<String, String> params = Map.of("createdAt_end", "invalid-date");
        assertThrows(BadRequestException.class, () -> 
            QueryFilter.fromQueryParams(params, List.of("createdat"), List.of(), false)
        );
    }

    @Test
    void testQueryFilter_UnallowedFilter() {
        Map<String, String> params = Map.of("name", "John");
        assertThrows(BadRequestException.class, () -> 
            QueryFilter.fromQueryParams(params, List.of("email"), List.of(), false)
        );
    }

    @Test
    void testQueryFilter_BooleanValue() {
        Map<String, String> params = Map.of("active", "true");
        QueryFilter filter = QueryFilter.fromQueryParams(params, List.of("active"), List.of(), false);
        assertEquals(1, filter.getAndRules().size());
        assertEquals(true, filter.getAndRules().get(0).getValue());
    }

    @Test
    void testQueryFilter_OnlyActiveFallback() {
        QueryFilter filter = QueryFilter.fromQueryParams(Map.of(), List.of("active"), List.of(), true);
        assertEquals(1, filter.getAndRules().size());
        assertEquals("active", filter.getAndRules().get(0).getField());
        assertEquals(true, filter.getAndRules().get(0).getValue());
    }

    @Test
    void testQueryFilter_GlobalSearch_NoSearchableFields() {
        Map<String, String> params = Map.of("searchWord", "test");
        assertThrows(BadRequestException.class, () -> 
            QueryFilter.fromQueryParams(params, List.of(), List.of(), false)
        );
    }

    @Test
    void testQueryFilter_GlobalSearch_MissingSearchFields() {
        Map<String, String> params = Map.of("searchWord", "test");
        assertThrows(BadRequestException.class, () -> 
            QueryFilter.fromQueryParams(params, List.of(), List.of("name"), false)
        );
    }

    @Test
    void testQueryFilter_GlobalSearch_UnallowedField() {
        Map<String, String> params = Map.of("searchWord", "test", "searchFields", "email");
        assertThrows(BadRequestException.class, () -> 
            QueryFilter.fromQueryParams(params, List.of(), List.of("name"), false)
        );
    }

    @Test
    void testQueryFilter_GlobalSearch_Success() {
        Map<String, String> params = Map.of("searchWord", "test", "searchFields", "name");
        QueryFilter filter = QueryFilter.fromQueryParams(params, List.of(), List.of("name"), false);
        assertEquals(1, filter.getOrRules().size());
        assertEquals("name", filter.getOrRules().get(0).getField());
        assertEquals("test", filter.getOrRules().get(0).getValue());
    }

    @Test
    void testQueryFilter_UnallowedOrderBy() {
        Map<String, String> params = Map.of("orderBy", "unallowed");
        assertThrows(BadRequestException.class, () -> 
            QueryFilter.fromQueryParams(params, List.of("name"), List.of(), false)
        );
    }
}
