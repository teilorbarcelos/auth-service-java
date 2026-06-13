package {{package}};

import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

public class {{module_name}}ServiceUnitTest {

    private {{module_name}}Service service;
    private {{module_name}}Repository repository;
    private EntityManager em;

    @BeforeEach
    void setup() {
        repository = mock({{module_name}}Repository.class);
        em = mock(EntityManager.class);
        service = new {{module_name}}Service(repository, em);
    }

    @Test
    void testRetrieveById() {
        {{module_name}}Model entity = new {{module_name}}Model();
        entity.setId("test-id");
        when(repository.findById("test-id")).thenReturn(entity);

        {{module_name}}Model result = service.retrieveById("test-id");
        assertNotNull(result);
        assertEquals("test-id", result.getId());
    }

    @Test
    void testCreate() {
        {{module_name}}Model entity = new {{module_name}}Model();
        {{module_name}}Model result = service.create(entity);
        assertNotNull(result);
        verify(repository).persist(entity);
    }

    @Test
    void testUpdate() {
        {{module_name}}Model existing = new {{module_name}}Model();
        existing.setId("test-id");
        when(repository.findById("test-id")).thenReturn(existing);

        {{module_name}}Model incoming = new {{module_name}}Model();
        
        {{module_name}}Model result = service.update("test-id", incoming);
        assertNotNull(result);
        verify(repository).persist(existing);
    }

    @Test
    void testDelete() {
        when(repository.softDelete("test-id")).thenReturn(true);
        assertTrue(service.delete("test-id"));
        verify(repository).softDelete("test-id");
    }

    @Test
    void testSetStatus() {
        {{module_name}}Model entity = new {{module_name}}Model();
        when(repository.setStatus("test-id", true)).thenReturn(entity);
        
        {{module_name}}Model result = service.setStatus("test-id", true);
        assertNotNull(result);
        verify(repository).setStatus("test-id", true);
    }

    @Test
    void testMergeFields() {
        {{module_name}}Model existing = new {{module_name}}Model();
        {{module_name}}Model incoming = new {{module_name}}Model();
        
{{test_merge_fields_setup}}

        service.mergeFields(existing, incoming);
        
{{test_merge_fields_assertions}}
    }

    @Test
    void testMergeFieldsWithNulls() {
        {{module_name}}Model existing = new {{module_name}}Model();
        existing.setActive(true);
        {{module_name}}Model incoming = new {{module_name}}Model();
        
        incoming.setActive(null);
        
        service.mergeFields(existing, incoming);
        
        assertTrue(existing.getActive());
    }
}
