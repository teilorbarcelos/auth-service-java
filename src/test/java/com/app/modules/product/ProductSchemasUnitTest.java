package com.app.modules.product;

import org.junit.jupiter.api.Test;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import static org.junit.jupiter.api.Assertions.*;

class ProductSchemasUnitTest {

    @Test
    void testResponseDto() {
        assertNotNull(new ProductSchemas());
        ProductSchemas.ResponseDto dto = new ProductSchemas.ResponseDto();
        dto.id = "1";
        dto.name = "Test";
        dto.description = "Desc";
        dto.price = BigDecimal.TEN;
        dto.stock = 10;
        dto.active = true;
        dto.createdAt = LocalDateTime.now();
        dto.updatedAt = LocalDateTime.now();

        assertEquals("1", dto.id);
        assertEquals("Test", dto.name);
        assertTrue(dto.active);
    }

    @Test
    void testListResponseDto() {
        ProductSchemas.ListResponseDto listDto = new ProductSchemas.ListResponseDto();
        listDto.items = List.of(new ProductSchemas.ResponseDto());
        listDto.total = 1L;
        listDto.page = 0;
        listDto.size = 25;

        assertNotNull(listDto.items);
        assertEquals(1L, listDto.total);
        assertEquals(0, listDto.page);
        assertEquals(25, listDto.size);
    }

    @Test
    void testRequestDto() {
        ProductSchemas.RequestDto dto = new ProductSchemas.RequestDto();
        dto.name = "Request";
        dto.active = true;
        assertEquals("Request", dto.name);
        assertTrue(dto.active);
    }

    @Test
    void testStatusRequest() {
        ProductSchemas.StatusRequest request = new ProductSchemas.StatusRequest(true);
        assertTrue(request.active());
        
        // Exercise record methods for coverage
        assertNotNull(request.toString());
        assertEquals(request, new ProductSchemas.StatusRequest(true));
        assertNotEquals(request, new ProductSchemas.StatusRequest(false));
        assertEquals(request.hashCode(), new ProductSchemas.StatusRequest(true).hashCode());
    }

    @Test
    void testProductModelUser() {
        ProductModel product = new ProductModel();
        assertNull(product.getUser());

        com.app.modules.user.UserModel user = new com.app.modules.user.UserModel();
        user.setId("user-1");

        product.setUser(user);
        assertEquals(user, product.getUser());
    }
}
