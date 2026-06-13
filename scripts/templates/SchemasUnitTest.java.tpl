package {{package}};

import org.junit.jupiter.api.Test;
import java.util.List;
import static org.junit.jupiter.api.Assertions.*;

class {{module_name}}SchemasUnitTest {

    @Test
    void testResponseDto() {
        assertNotNull(new {{module_name}}Schemas());
        {{module_name}}Schemas.ResponseDto dto = new {{module_name}}Schemas.ResponseDto();
        dto.id = "test-id";
        dto.active = true;
        
        assertEquals("test-id", dto.id);
        assertTrue(dto.active);
    }

    @Test
    void testListResponseDto() {
        {{module_name}}Schemas.ListResponseDto listDto = new {{module_name}}Schemas.ListResponseDto();
        listDto.items = List.of(new {{module_name}}Schemas.ResponseDto());
        listDto.total = 1L;
        listDto.page = 0;
        listDto.size = 25;

        assertEquals(1L, listDto.total);
        assertEquals(0, listDto.page);
        assertEquals(25, listDto.size);
        assertNotNull(listDto.items);
    }

    @Test
    void testRequestDto() {
        {{module_name}}Schemas.RequestDto dto = new {{module_name}}Schemas.RequestDto();
        dto.active = true;
        assertTrue(dto.active);
    }

    @Test
    void testStatusRequest() {
        {{module_name}}Schemas.StatusRequest request = new {{module_name}}Schemas.StatusRequest(true);
        assertTrue(request.active());
        assertNotNull(request.toString());
        assertEquals(request, new {{module_name}}Schemas.StatusRequest(true));
        assertEquals(request.hashCode(), new {{module_name}}Schemas.StatusRequest(true).hashCode());
    }
}
