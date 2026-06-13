package com.app.core;

/**
 * Represents a single filter rule for querying.
 */
public class FilterRule {
    public enum Operator {
        EQUALS,
        GREATER_THAN_OR_EQUAL,
        LESS_THAN_OR_EQUAL,
        LIKE
    }

    private final String field;
    private final Object value;
    private final Operator operator;

    public FilterRule(String field, Object value, Operator operator) {
        this.field = field;
        this.value = value;
        this.operator = operator;
    }

    public String getField() { return field; }
    public Object getValue() { return value; }
    public Operator getOperator() { return operator; }
}
