package com.app.modules.feature;

import org.junit.jupiter.api.Test;
import java.util.List;
import static org.junit.jupiter.api.Assertions.*;

class FeatureSchemasUnitTest {

    @Test
    void testResponseDto() {
        // Exercise constructor for coverage
        assertNotNull(new FeatureSchemas());
        
        FeatureSchemas.ResponseDto dto = new FeatureSchemas.ResponseDto();
        dto.id = "f1";
        dto.name = "Feature";
        dto.active = true;
        assertEquals("f1", dto.id);
        assertTrue(dto.active);
    }

    @Test
    void testListResponseDto() {
        FeatureSchemas.ListResponseDto listDto = new FeatureSchemas.ListResponseDto();
        listDto.items = List.of(new FeatureSchemas.ResponseDto());
        listDto.total = 100L;
        assertEquals(100L, listDto.total);
    }

    @Test
    void testStatusRequest() {
        FeatureSchemas.StatusRequest request = new FeatureSchemas.StatusRequest(false);
        assertFalse(request.active());
        assertNotNull(request.toString());
    }
}
