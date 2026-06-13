package com.app.core;

import com.app.core.dto.PaginatedResponse;
import jakarta.persistence.EntityManager;
import jakarta.persistence.TypedQuery;
import jakarta.persistence.criteria.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Helper class to build and execute JPA Criteria searches based on QueryFilter.
 */
public class SearchQueryBuilder {

    private SearchQueryBuilder() {
    }

    public static <E extends BaseEntity> PaginatedResponse<E> buildAndExecute(
            EntityManager em,
            Class<E> entityClass,
            QueryFilter filter) {

        CriteriaBuilder cb = em.getCriteriaBuilder();

        // --- Count query ---
        CriteriaQuery<Long> countQuery = cb.createQuery(Long.class);
        Root<E> countRoot = countQuery.from(entityClass);
        countQuery.select(cb.count(countRoot));
        List<Predicate> countPredicates = buildPredicates(cb, countRoot, filter.getAndRules(), filter.getOrRules());
        countQuery.where(countPredicates.toArray(new Predicate[0]));
        long total = em.createQuery(countQuery).getSingleResult();

        // --- Data query ---
        CriteriaQuery<E> dataQuery = cb.createQuery(entityClass);
        Root<E> dataRoot = dataQuery.from(entityClass);
        dataQuery.select(dataRoot);
        List<Predicate> dataPredicates = buildPredicates(cb, dataRoot, filter.getAndRules(), filter.getOrRules());
        dataQuery.where(dataPredicates.toArray(new Predicate[0]));

        String orderBy = filter.getOrderBy();
        String orderDirection = filter.getOrderDirection();
        if ("desc".equalsIgnoreCase(orderDirection)) {
            dataQuery.orderBy(cb.desc(resolveOrderPath(dataRoot, orderBy)));
        } else {
            dataQuery.orderBy(cb.asc(resolveOrderPath(dataRoot, orderBy)));
        }

        TypedQuery<E> typedQuery = em.createQuery(dataQuery);
        typedQuery.setFirstResult(filter.getPage() * filter.getSize());
        typedQuery.setMaxResults(filter.getSize());

        List<E> items = typedQuery.getResultList();
        return new PaginatedResponse<>(items, total, filter.getPage(), filter.getSize());
    }

    public static <E extends BaseEntity> List<E> buildAndExecuteAll(
            EntityManager em,
            Class<E> entityClass,
            QueryFilter filter) {

        CriteriaBuilder cb = em.getCriteriaBuilder();

        // --- Data query ---
        CriteriaQuery<E> dataQuery = cb.createQuery(entityClass);
        Root<E> dataRoot = dataQuery.from(entityClass);
        dataQuery.select(dataRoot);
        List<Predicate> dataPredicates = buildPredicates(cb, dataRoot, filter.getAndRules(), filter.getOrRules());
        dataQuery.where(dataPredicates.toArray(new Predicate[0]));

        String orderBy = filter.getOrderBy();
        String orderDirection = filter.getOrderDirection();
        if ("desc".equalsIgnoreCase(orderDirection)) {
            dataQuery.orderBy(cb.desc(resolveOrderPath(dataRoot, orderBy)));
        } else {
            dataQuery.orderBy(cb.asc(resolveOrderPath(dataRoot, orderBy)));
        }

        TypedQuery<E> typedQuery = em.createQuery(dataQuery);
        return typedQuery.getResultList();
    }

    private static <E extends BaseEntity> List<Predicate> buildPredicates(
            CriteriaBuilder cb,
            Root<E> root,
            List<FilterRule> andRules,
            List<FilterRule> orRules) {
        List<Predicate> predicates = new ArrayList<>();

        predicates.add(cb.or(
                cb.isNull(root.get("isDeleted")),
                cb.equal(root.get("isDeleted"), false)));

        for (FilterRule rule : andRules) {
            Path<?> path = resolvePath(root, rule.getField());
            Object value = rule.getValue();

            FilterRule.Operator op = rule.getOperator();
            if (op == FilterRule.Operator.GREATER_THAN_OR_EQUAL) {
                predicates.add(cb.greaterThanOrEqualTo(path.as(LocalDateTime.class), parseDate(value, false)));
            } else if (op == FilterRule.Operator.LESS_THAN_OR_EQUAL) {
                predicates.add(cb.lessThanOrEqualTo(path.as(LocalDateTime.class), parseDate(value, true)));
            } else if (op == FilterRule.Operator.EQUALS) {
                predicates.add(cb.equal(path, value));
            }
        }

        if (!orRules.isEmpty()) {
            List<Predicate> orPredicates = new ArrayList<>();
            for (FilterRule rule : orRules) {
                Object value = rule.getValue();
                String rawValue = value != null ? value.toString() : null;
                String normalizedValue = "%" + removeAccents(rawValue).toLowerCase() + "%";

                Expression<String> path = resolvePath(root, rule.getField()).as(String.class);

                Expression<String> unaccentPath = cb.function("immutable_unaccent", String.class, cb.lower(path));
                orPredicates.add(cb.like(unaccentPath, normalizedValue));
            }
            predicates.add(cb.or(orPredicates.toArray(new Predicate[0])));
        }

        return predicates;
    }

    private static String removeAccents(String value) {
        if (value == null)
            return "";
        String normalized = java.text.Normalizer.normalize(value, java.text.Normalizer.Form.NFD);
        return normalized.replaceAll("\\p{M}", "");
    }

    private static LocalDateTime parseDate(Object value, boolean isEnd) {
        if (value == null)
            return null;
        if (value instanceof LocalDateTime)
            return (LocalDateTime) value;
        String str = value.toString();
        try {
            if (str.length() == 10) {
                return LocalDateTime.parse(str + (isEnd ? "T23:59:59" : "T00:00:00"));
            }
            return LocalDateTime.parse(str);
        } catch (Exception e) {
            return null;
        }
    }

    private static <E extends BaseEntity> Path<?> resolvePath(Root<E> root, String field) {
        if (field.contains(".")) {
            String[] parts = field.split("\\.", 2);
            Join<?, ?> join = root.join(parts[0], JoinType.LEFT);
            return join.get(parts[1]);
        }
        return root.get(field);
    }

    private static <E extends BaseEntity> Path<?> resolveOrderPath(Root<E> root, String orderBy) {
        return resolvePath(root, orderBy);
    }
}
