package com.app.modules.dashboard;

import org.junit.jupiter.api.Test;
import java.util.List;
import static org.junit.jupiter.api.Assertions.*;

class DashboardSchemasUnitTest {

    @Test
    void testTimeSeriesStatDto() {
        assertNotNull(new DashboardSchemas());
        DashboardSchemas.TimeSeriesStatDto dto = new DashboardSchemas.TimeSeriesStatDto();
        dto.setDate("2026-05-23");
        dto.setCount(15);

        assertEquals("2026-05-23", dto.getDate());
        assertEquals(15, dto.getCount());

        DashboardSchemas.TimeSeriesStatDto paramDto = new DashboardSchemas.TimeSeriesStatDto("2026-05-24", 25);
        assertEquals("2026-05-24", paramDto.getDate());
        assertEquals(25, paramDto.getCount());
    }

    @Test
    void testUserProductStatDto() {
        DashboardSchemas.UserProductStatDto dto = new DashboardSchemas.UserProductStatDto();
        dto.setUserId("user-123");
        dto.setUserName("John Doe");
        dto.setCount(5);

        assertEquals("user-123", dto.getUserId());
        assertEquals("John Doe", dto.getUserName());
        assertEquals(5, dto.getCount());

        DashboardSchemas.UserProductStatDto paramDto = new DashboardSchemas.UserProductStatDto("user-456", "Jane Doe", 10);
        assertEquals("user-456", paramDto.getUserId());
        assertEquals("Jane Doe", paramDto.getUserName());
        assertEquals(10, paramDto.getCount());
    }

    @Test
    void testDashboardStatsResponseDto() {
        DashboardSchemas.DashboardStatsResponseDto dto = new DashboardSchemas.DashboardStatsResponseDto();
        assertNull(dto.getUserCreationStats());
        assertNull(dto.getProductCreationStats());
        assertNull(dto.getProductsPerUser());

        List<DashboardSchemas.TimeSeriesStatDto> userStats = List.of(new DashboardSchemas.TimeSeriesStatDto("2026-05-23", 5));
        List<DashboardSchemas.TimeSeriesStatDto> productStats = List.of(new DashboardSchemas.TimeSeriesStatDto("2026-05-23", 10));
        List<DashboardSchemas.UserProductStatDto> productsPerUser = List.of(new DashboardSchemas.UserProductStatDto("user-123", "John Doe", 5));

        dto.setUserCreationStats(userStats);
        dto.setProductCreationStats(productStats);
        dto.setProductsPerUser(productsPerUser);

        assertEquals(userStats, dto.getUserCreationStats());
        assertEquals(productStats, dto.getProductCreationStats());
        assertEquals(productsPerUser, dto.getProductsPerUser());
    }
}
