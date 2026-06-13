package com.app.core;

import com.app.core.dto.ErrorResponse;
import com.app.core.dto.PaginatedResponse;
import com.app.modules.RootResource;
import com.app.modules.debug.DebugResource;
import io.quarkus.test.junit.QuarkusTest;
import jakarta.inject.Inject;
import jakarta.ws.rs.core.Response;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

@QuarkusTest
public class MiscTest {

    @Inject
    RootResource rootResource;

    @Inject
    DebugResource debugResource;

    @Test
    @SuppressWarnings("unchecked")
    void testRootResource() {
        Response response = rootResource.root();
        assertEquals(200, response.getStatus());
        Map<String, String> entity = (Map<String, String>) response.getEntity();
        assertEquals("Backend Java Quarkus", entity.get("name"));
    }

    @Test
    void testDebugResource() {
        assertThrows(RuntimeException.class, () -> debugResource.triggerError());
    }

    @Test
    @SuppressWarnings("unchecked")
    void testErrorResponseDTO() {
        ErrorResponse err = new ErrorResponse("msg", "code");
        assertEquals("msg", err.getError().getMessage());
        assertEquals("code", err.getError().getCode());
        assertNull(err.getError().getDetails());

        ErrorResponse err2 = new ErrorResponse("msg", "code", Map.of("k", "v"));
        assertEquals("v", ((Map<String, Object>)err2.getError().getDetails()).get("k"));
    }

    @Test
    void testPaginatedResponseDTO() {
        PaginatedResponse<String> resp = new PaginatedResponse<>(List.of("a"), 100, 1, 10);
        assertEquals(1, resp.getItems().size());
        assertEquals(1, resp.getPage());
        assertEquals(10, resp.getSize());
        assertEquals(100, resp.getTotal());
        
        resp.setItems(List.of("b"));
        resp.setPage(2);
        resp.setSize(20);
        resp.setTotal(200);
        
        assertEquals("b", resp.getItems().get(0));
        assertEquals(2, resp.getPage());
        assertEquals(20, resp.getSize());
        assertEquals(200, resp.getTotal());
    }

    @Test
    void testBaseEntity() {
        // Since BaseEntity is abstract, we test it through a subclass or directly if possible
        BaseEntity entity = new BaseEntity() {};
        entity.setId("test-id");
        LocalDateTime now = LocalDateTime.now();
        entity.setCreatedAt(now);
        entity.setUpdatedAt(now);

        assertEquals("test-id", entity.getId());
        assertEquals(now, entity.getCreatedAt());
        assertEquals(now, entity.getUpdatedAt());
    }
}
