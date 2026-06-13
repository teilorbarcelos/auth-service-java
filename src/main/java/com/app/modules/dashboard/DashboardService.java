package com.app.modules.dashboard;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.persistence.EntityManager;
import jakarta.persistence.Query;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

import com.app.modules.dashboard.DashboardSchemas.*;

@ApplicationScoped
public class DashboardService {

    @Inject
    EntityManager em;

    public DashboardStatsResponseDto getStats(String createdAtStart, String createdAtEnd) {
        LocalDateTime start = parseStartDate(createdAtStart);
        LocalDateTime end = parseEndDate(createdAtEnd);

        DashboardStatsResponseDto response = new DashboardStatsResponseDto();
        response.userCreationStats = getUserCreationStats(start, end);
        response.productCreationStats = getProductCreationStats(start, end);
        response.productsPerUser = getProductsPerUser(start, end);

        return response;
    }

    @SuppressWarnings("unchecked")
    private List<TimeSeriesStatDto> getUserCreationStats(LocalDateTime start, LocalDateTime end) {
        String sql = "SELECT TO_CHAR(created_at, 'YYYY-MM-DD') AS date, COUNT(*) AS count " +
                     "FROM users " +
                     "WHERE created_at >= :start AND created_at <= :end AND is_deleted = false " +
                     "GROUP BY TO_CHAR(created_at, 'YYYY-MM-DD') " +
                     "ORDER BY date ASC";
        
        Query query = em.createNativeQuery(sql);
        query.setParameter("start", start);
        query.setParameter("end", end);

        List<Object[]> rows = query.getResultList();
        List<TimeSeriesStatDto> result = new ArrayList<>();
        for (Object[] row : rows) {
            String date = (String) row[0];
            Number count = (Number) row[1];
            result.add(new TimeSeriesStatDto(date, count.intValue()));
        }
        return result;
    }

    @SuppressWarnings("unchecked")
    private List<TimeSeriesStatDto> getProductCreationStats(LocalDateTime start, LocalDateTime end) {
        String sql = "SELECT TO_CHAR(created_at, 'YYYY-MM-DD') AS date, COUNT(*) AS count " +
                     "FROM products " +
                     "WHERE created_at >= :start AND created_at <= :end AND is_deleted = false " +
                     "GROUP BY TO_CHAR(created_at, 'YYYY-MM-DD') " +
                     "ORDER BY date ASC";
        
        Query query = em.createNativeQuery(sql);
        query.setParameter("start", start);
        query.setParameter("end", end);

        List<Object[]> rows = query.getResultList();
        List<TimeSeriesStatDto> result = new ArrayList<>();
        for (Object[] row : rows) {
            String date = (String) row[0];
            Number count = (Number) row[1];
            result.add(new TimeSeriesStatDto(date, count.intValue()));
        }
        return result;
    }

    @SuppressWarnings("unchecked")
    private List<UserProductStatDto> getProductsPerUser(LocalDateTime start, LocalDateTime end) {
        String sql = "SELECT p.id_user AS userId, COALESCE(u.name, 'Anonymous') AS userName, COUNT(*) AS count " +
                     "FROM products p " +
                     "LEFT JOIN users u ON p.id_user = u.id " +
                     "WHERE p.created_at >= :start AND p.created_at <= :end AND p.is_deleted = false " +
                     "GROUP BY p.id_user, u.name " +
                     "ORDER BY count DESC";

        Query query = em.createNativeQuery(sql);
        query.setParameter("start", start);
        query.setParameter("end", end);

        List<Object[]> rows = query.getResultList();
        List<UserProductStatDto> result = new ArrayList<>();
        for (Object[] row : rows) {
            String userId = (String) row[0];
            String userName = (String) row[1];
            Number count = (Number) row[2];
            result.add(new UserProductStatDto(userId, userName, count.intValue()));
        }
        return result;
    }

    public static LocalDateTime parseStartDate(String dateStr) {
        if (dateStr == null || dateStr.trim().isEmpty()) {
            return LocalDateTime.now().minusDays(30);
        }
        try {
            java.time.LocalDate parsed = java.time.LocalDate.parse(dateStr, DateTimeFormatter.ISO_LOCAL_DATE);
            return parsed.atStartOfDay();
        } catch (Exception e) {
            return LocalDateTime.now().minusDays(30);
        }
    }

    public static LocalDateTime parseEndDate(String dateStr) {
        if (dateStr == null || dateStr.trim().isEmpty()) {
            return LocalDateTime.now();
        }
        try {
            java.time.LocalDate parsed = java.time.LocalDate.parse(dateStr, DateTimeFormatter.ISO_LOCAL_DATE);
            return parsed.atTime(23, 59, 59);
        } catch (Exception e) {
            return LocalDateTime.now();
        }
    }
}
