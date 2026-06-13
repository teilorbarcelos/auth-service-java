package com.app.core.dto;

import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

public class CoreDtoUnitTest {

    @Test
    void testErrorResponse() {
        // Test with details
        ErrorResponse resp1 = new ErrorResponse("msg", "CODE", Map.of("k", "v"));
        assertFalse(resp1.isSuccess());
        assertEquals("msg", resp1.getError().getMessage());
        assertEquals("CODE", resp1.getError().getCode());
        assertNotNull(resp1.getError().getDetails());

        // Test without details
        ErrorResponse resp2 = new ErrorResponse("msg", "CODE");
        assertNull(resp2.getError().getDetails());

        // Test default constructor for Jackson
        ErrorResponse resp3 = new ErrorResponse();
        assertNull(resp3.getError());
        
        ErrorResponse.ErrorDetail detail = new ErrorResponse.ErrorDetail();
        assertNull(detail.getCode());
    }

    @Test
    void testPaginatedResponse() {
        List<String> items = List.of("a", "b");
        PaginatedResponse<String> resp = new PaginatedResponse<>(items, 10, 1, 2);
        
        assertEquals(items, resp.getItems());
        assertEquals(10, resp.getTotal());
        assertEquals(1, resp.getPage());
        assertEquals(2, resp.getSize());

        // Test setters and default constructor
        PaginatedResponse<String> resp2 = new PaginatedResponse<>();
        resp2.setItems(items);
        resp2.setTotal(5);
        resp2.setPage(2);
        resp2.setSize(3);

        assertEquals(items, resp2.getItems());
        assertEquals(5, resp2.getTotal());
        assertEquals(2, resp2.getPage());
        assertEquals(3, resp2.getSize());
    }
}
