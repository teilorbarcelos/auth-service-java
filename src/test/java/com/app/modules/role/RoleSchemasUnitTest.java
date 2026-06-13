package com.app.modules.role;

import org.junit.jupiter.api.Test;
import java.time.LocalDateTime;
import java.util.List;
import static org.junit.jupiter.api.Assertions.*;

class RoleSchemasUnitTest {

    @Test
    void testResponseDto() {
        assertNotNull(new RoleSchemas());
        RoleSchemas.ResponseDto dto = new RoleSchemas.ResponseDto();
        dto.id = "r1";
        dto.name = "Admin";
        dto.description = "Full access";
        dto.active = true;
        dto.createdAt = LocalDateTime.now();
        dto.features = List.of(new RoleSchemas.RoleFeatureResponse());

        assertEquals("Admin", dto.name);
        assertNotNull(dto.features);
    }

    @Test
    void testListResponseDto() {
        RoleSchemas.ListResponseDto listDto = new RoleSchemas.ListResponseDto();
        listDto.items = List.of(new RoleSchemas.ResponseDto());
        listDto.total = 10L;
        listDto.page = 1;
        listDto.size = 5;

        assertEquals(10L, listDto.total);
        assertEquals(1, listDto.page);
    }

    @Test
    void testRequestDto() {
        RoleSchemas.RequestDto dto = new RoleSchemas.RequestDto();
        dto.name = "Manager";
        dto.permissions = List.of(new RoleSchemas.PermissionRequest());
        assertEquals("Manager", dto.name);
        assertNotNull(dto.permissions);
    }

    @Test
    void testPermissionRequest() {
        RoleSchemas.PermissionRequest req = new RoleSchemas.PermissionRequest();
        req.id_feature = "user";
        req.view = true;
        req.create = false;
        assertEquals("user", req.id_feature);
        assertTrue(req.view);
        assertFalse(req.create);
    }

    @Test
    void testRoleFeatureResponse() {
        RoleSchemas.RoleFeatureResponse res = new RoleSchemas.RoleFeatureResponse();
        res.idFeature = "product";
        res.permissions = "{\"view\": true}";
        assertEquals("product", res.idFeature);
        assertEquals("{\"view\": true}", res.permissions);
    }

    @Test
    void testStatusRequest() {
        RoleSchemas.StatusRequest request = new RoleSchemas.StatusRequest(true);
        assertTrue(request.active());
        assertNotNull(request.toString());
    }
}
