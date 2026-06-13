package com.app.core.dto;

import org.junit.jupiter.api.Test;
import java.util.List;
import static org.junit.jupiter.api.Assertions.*;

class PaginatedResponseUnitTest {

    @Test
    void testPaginatedResponse() {
        List<String> items = List.of("A", "B");
        PaginatedResponse<String> response = new PaginatedResponse<>(items, 10, 1, 25);

        assertEquals(items, response.getItems());
        assertEquals(10, response.getTotal());
        assertEquals(1, response.getPage());
        assertEquals(25, response.getSize());
    }

    @Test
    void testSetters() {
        PaginatedResponse<Integer> response = new PaginatedResponse<>();
        List<Integer> items = List.of(1, 2);
        
        response.setItems(items);
        response.setTotal(100L);
        response.setPage(5);
        response.setSize(50);

        assertEquals(items, response.getItems());
        assertEquals(100L, response.getTotal());
        assertEquals(5, response.getPage());
        assertEquals(50, response.getSize());
    }
}
