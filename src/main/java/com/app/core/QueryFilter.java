package com.app.core;

import com.app.core.exception.BadRequestException;
import java.util.*;

/**
 * Encapsulates filtering, searching, and pagination parameters.
 */
public class QueryFilter {
    private final int page;
    private final int size;
    private final List<FilterRule> andRules;
    private final List<FilterRule> orRules;
    private final String orderBy;
    private final String orderDirection;

    public QueryFilter(int page, int size, List<FilterRule> andRules, List<FilterRule> orRules, String orderBy,
            String orderDirection) {
        this.page = page;
        this.size = size;
        this.andRules = andRules;
        this.orRules = orRules;
        this.orderBy = orderBy;
        this.orderDirection = orderDirection;
    }

    public static QueryFilter fromQueryParams(
            Map<String, String> queryParams,
            List<String> filterableFields,
            List<String> searchableFields,
            boolean onlyActive) {

        int page = parseIntOrDefault(queryParams.get("page"), 0);
        int size = parseIntOrDefault(queryParams.get("size"), 10);
        if (size > 100) {
            throw new BadRequestException("O tamanho da página não pode ser maior que 100.");
        }
        String searchWord = queryParams.get("searchWord");
        String searchFields = queryParams.get("searchFields");
        String orderBy = queryParams.getOrDefault("orderBy", "createdAt");
        String orderDirection = queryParams.getOrDefault("orderDirection", "desc");

        Set<String> reservedParams = Set.of("page", "size", "searchWord", "searchFields", "orderBy", "orderDirection");

        List<FilterRule> andRules = new ArrayList<>();
        Set<String> activeFilterFields = new HashSet<>();

        for (Map.Entry<String, String> entry : queryParams.entrySet()) {
            String key = entry.getKey();
            String value = entry.getValue();

            if (reservedParams.contains(key))
                continue;
            if (value == null || value.isBlank())
                continue;

            String normalizedKey = key.contains(".") ? key.toLowerCase() : key;
            String baseKey = normalizedKey;
            FilterRule.Operator operator = FilterRule.Operator.EQUALS;

            if (normalizedKey.endsWith("_start")) {
                try {
                    java.time.LocalDate.parse(value, java.time.format.DateTimeFormatter.ISO_LOCAL_DATE);
                } catch (java.time.format.DateTimeParseException e) {
                    throw new BadRequestException("Formato de data inválido para '" + key + "'. Use YYYY-MM-DD.");
                }
                baseKey = normalizedKey.substring(0, normalizedKey.length() - 6);
                operator = FilterRule.Operator.GREATER_THAN_OR_EQUAL;
            } else if (normalizedKey.endsWith("_end")) {
                try {
                    java.time.LocalDate.parse(value, java.time.format.DateTimeFormatter.ISO_LOCAL_DATE);
                } catch (java.time.format.DateTimeParseException e) {
                    throw new BadRequestException("Formato de data inválido para '" + key + "'. Use YYYY-MM-DD.");
                }
                baseKey = normalizedKey.substring(0, normalizedKey.length() - 4);
                operator = FilterRule.Operator.LESS_THAN_OR_EQUAL;
            }

            if (!filterableFields.contains(baseKey)) {
                throw new BadRequestException("O filtro '" + key + "' não é permitido para este recurso.");
            }

            activeFilterFields.add(baseKey);

            Object finalValue;
            if ("true".equalsIgnoreCase(value) || "false".equalsIgnoreCase(value)) {
                finalValue = Boolean.parseBoolean(value);
            } else {
                finalValue = value;
            }

            andRules.add(new FilterRule(baseKey, finalValue, operator));
        }

        if (onlyActive && filterableFields.contains("active") && !activeFilterFields.contains("active")) {
            andRules.add(new FilterRule("active", true, FilterRule.Operator.EQUALS));
        }

        List<FilterRule> orRules = new ArrayList<>();
        if (searchWord != null && !searchWord.isBlank()) {
            if (searchableFields.isEmpty()) {
                throw new BadRequestException("A pesquisa global (searchWord) não está habilitada para este recurso.");
            }
            if (searchFields == null || searchFields.isBlank()) {
                throw new BadRequestException(
                        "O parâmetro \"searchFields\" é obrigatório quando \"searchWord\" é fornecido.");
            }

            String[] requestedFields = searchFields.split(",");
            for (String field : requestedFields) {
                String trimmed = field.trim();
                String normalizedField = trimmed.contains(".") ? trimmed.toLowerCase() : trimmed;
                if (!searchableFields.contains(normalizedField)) {
                    throw new BadRequestException(
                            "O campo '" + trimmed + "' não está disponível para pesquisa global.");
                }
                orRules.add(new FilterRule(normalizedField, searchWord, FilterRule.Operator.LIKE));
            }
        }

        if (!"createdAt".equals(orderBy) && !"updatedAt".equals(orderBy)) {
            if (!filterableFields.contains(orderBy)) {
                throw new BadRequestException("A ordenação pelo campo '" + orderBy + "' não é permitida.");
            }
        }

        return new QueryFilter(page, size, andRules, orRules, orderBy, orderDirection);
    }

    private static int parseIntOrDefault(String value, int defaultValue) {
        if (value == null)
            return defaultValue;
        try {
            return Integer.parseInt(value);
        } catch (NumberFormatException e) {
            return defaultValue;
        }
    }

    public int getPage() {
        return page;
    }

    public int getSize() {
        return size;
    }

    public List<FilterRule> getAndRules() {
        return andRules;
    }

    public List<FilterRule> getOrRules() {
        return orRules;
    }

    public String getOrderBy() {
        return orderBy;
    }

    public String getOrderDirection() {
        return orderDirection;
    }
}
