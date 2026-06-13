package com.app.core;

import com.app.modules.product.ProductModel;
import com.app.modules.product.ProductService;
import io.quarkus.test.InjectMock;
import io.quarkus.test.junit.QuarkusTest;
import jakarta.inject.Inject;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.UriInfo;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.util.Collections;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;

@QuarkusTest
public class BaseResourceTest {

    @Inject
    com.app.modules.product.ProductResource resource;

    @InjectMock
    ProductService service;

    @Test
    @DisplayName("Should handle 404 branches in all endpoints")
    public void test404Branches() {
        Mockito.when(service.retrieveById(anyString())).thenReturn(null);
        Mockito.when(service.update(anyString(), any())).thenReturn(null);
        Mockito.when(service.delete(anyString())).thenReturn(false);
        Mockito.when(service.setStatus(anyString(), anyBoolean())).thenReturn(null);

        ProductModel validProduct = new ProductModel();
        validProduct.setName("Test");
        validProduct.setSku("TEST-SKU");
        validProduct.setPrice(new java.math.BigDecimal("10.0"));

        assertEquals(404, resource.getById("id").getStatus());
        assertEquals(404, resource.update("id", validProduct).getStatus());
        assertEquals(404, resource.delete("id").getStatus());
        assertEquals(404, resource.toggleStatus("id", null).getStatus());
    }

    @Test
    @DisplayName("Should handle toggleStatus with and without active key")
    public void testToggleStatusBranches() {
        ProductModel p = new ProductModel();
        Mockito.when(service.setStatus(anyString(), anyBoolean())).thenReturn(p);

        // Record with active = false
        Response resp1 = resource.toggleStatus("id", new com.app.modules.product.ProductSchemas.StatusRequest(false));
        assertEquals(200, resp1.getStatus());
        Mockito.verify(service).setStatus("id", false);

        // Null body (should default to true in internalToggleStatus)
        Response resp2 = resource.toggleStatus("id", null);
        assertEquals(200, resp2.getStatus());
        Mockito.verify(service).setStatus("id", true);

        // Record with active = true
        Response resp3 = resource.toggleStatus("id", new com.app.modules.product.ProductSchemas.StatusRequest(true));
        assertEquals(200, resp3.getStatus());
        Mockito.verify(service, Mockito.times(2)).setStatus("id", true);
    }

    @Test
    @DisplayName("Should handle listItems with query parameters including empty values")
    public void testListItems() {
        UriInfo uriInfo = Mockito.mock(UriInfo.class);
        jakarta.ws.rs.core.MultivaluedMap<String, String> params = new jakarta.ws.rs.core.MultivaluedHashMap<>();
        params.add("page", "1");
        params.put("empty", Collections.emptyList()); // Covers line 95 (if !values.isEmpty()) == false
        
        Mockito.when(uriInfo.getQueryParameters()).thenReturn(params);
        
        Mockito.when(service.listItems(anyMap(), anyBoolean())).thenReturn(new com.app.core.dto.PaginatedResponse<>(List.of(), 0, 0, 10));
        Mockito.when(service.listAllItems(anyMap())).thenReturn(new com.app.core.dto.PaginatedResponse<>(List.of(), 0, 0, 10));
        
        assertEquals(200, resource.listItems(uriInfo).getStatus());
        assertEquals(200, resource.listAllItems(uriInfo).getStatus());
    }

    @Test
    @DisplayName("Should handle create endpoint")
    public void testCreate() {
        ProductModel validProduct = new ProductModel();
        validProduct.setName("Test");
        validProduct.setSku("TEST-SKU");
        validProduct.setPrice(new java.math.BigDecimal("10.0"));
        
        Mockito.when(service.create(any())).thenReturn(validProduct);
        
        assertEquals(201, resource.create(validProduct).getStatus());
    }
}
