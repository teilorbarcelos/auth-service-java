package com.app.modules.user;

import org.junit.jupiter.api.Test;
import java.time.LocalDateTime;
import java.util.List;
import static org.junit.jupiter.api.Assertions.*;

class UserSchemasUnitTest {

    @Test
    void testResponseDto() {
        assertNotNull(new UserSchemas());
        UserSchemas.ResponseDto dto = new UserSchemas.ResponseDto();
        dto.id = "1";
        dto.name = "User";
        dto.email = "test@test.com";
        dto.active = true;
        dto.createdAt = LocalDateTime.now();
        dto.updatedAt = LocalDateTime.now();

        assertEquals("User", dto.name);
        assertTrue(dto.active);
    }

    @Test
    void testListResponseDto() {
        UserSchemas.ListResponseDto listDto = new UserSchemas.ListResponseDto();
        listDto.items = List.of(new UserSchemas.ResponseDto());
        listDto.total = 1L;
        listDto.page = 0;
        listDto.size = 25;

        assertEquals(1L, listDto.total);
    }

    @Test
    void testRequestDto() {
        UserSchemas.RequestDto dto = new UserSchemas.RequestDto();
        dto.name = "Req";
        dto.email = "req@test.com";
        dto.active = true;
        assertEquals("Req", dto.name);
    }

    @Test
    void testStatusRequest() {
        UserSchemas.StatusRequest request = new UserSchemas.StatusRequest(true);
        assertTrue(request.active());
        assertNotNull(request.toString());
    }
}
