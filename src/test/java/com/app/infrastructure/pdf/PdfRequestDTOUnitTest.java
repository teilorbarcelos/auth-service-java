package com.app.infrastructure.pdf;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
import java.util.HashMap;
import java.util.Map;

public class PdfRequestDTOUnitTest {

    @Test
    void testGettersAndSetters() {
        PdfRequestDTO dto = new PdfRequestDTO();
        dto.setTemplate("test-template");
        
        Map<String, Object> data = new HashMap<>();
        data.put("key", "value");
        dto.setData(data);

        PdfRequestDTO.Options options = new PdfRequestDTO.Options();
        options.setLandscape(true);
        options.setFormat("Letter");
        dto.setOptions(options);

        assertEquals("test-template", dto.getTemplate());
        assertEquals(data, dto.getData());
        assertNotNull(dto.getOptions());
        assertTrue(dto.getOptions().isLandscape());
        assertEquals("Letter", dto.getOptions().getFormat());
    }

    @Test
    void testDefaultValues() {
        PdfRequestDTO dto = new PdfRequestDTO();
        assertNull(dto.getTemplate());
        assertNull(dto.getData());
        assertNull(dto.getOptions());
        
        PdfRequestDTO.Options options = new PdfRequestDTO.Options();
        assertFalse(options.isLandscape());
        assertEquals("A4", options.getFormat());
    }
}
