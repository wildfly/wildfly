/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.wildfly.extension.observability.shared;

import java.util.Arrays;
import java.util.Map;
import java.util.stream.Collectors;

import org.jboss.dmr.ModelNode;

/**
 * Model representing a meter filter configuration that can accept or reject metrics based on
 * meter name, tag name, or tag value.
 *
 * @param outcome the filtering outcome (accept or reject)
 * @param field the field to filter on (meter name, tag name, or tag value)
 * @param condition the matching condition (starts with, ends with, contains, or equals)
 * @param negate whether to invert the match result
 * @param value the value to match against
 */
public record FilterModel(Outcome outcome, Field field, Condition condition, boolean negate, String value) {

    /**
     * Creates a FilterModel from a DMR ModelNode representation.
     *
     * @param model the ModelNode containing filter configuration
     * @return a new FilterModel instance
     */
    public static FilterModel of(ModelNode model) {
        return new FilterModel(Outcome.of(model.get("outcome").asString("reject")),
                Field.of(model.get("field").asString()),
                Condition.of(model.get("condition").asString()),
                model.get("negate").asBoolean(false),
                model.get("value").asString());
    }

    /**
     * Determines if this filter matches the given meter name and tags.
     *
     * @param name the meter name
     * @param tags the meter tags
     * @return true if the filter matches
     */
    public boolean matches(String name, Map<String, String> tags) {
        boolean result = switch (field) {
            case METER_NAME -> matchesCondition(condition, name, value);
            case TAG_NAME -> tags.keySet().stream()
                    .anyMatch(key -> matchesCondition(condition, key, value));
            case TAG_VALUE -> tags.values().stream()
                    .anyMatch(val -> matchesCondition(condition, val, value));
        };
        return negate ? !result : result;
    }

    /**
     * Tests if a target string matches a value according to the given condition.
     *
     * @param condition the matching condition
     * @param target the string to test
     * @param value the value to match against
     * @return true if the condition matches
     */
    static boolean matchesCondition(Condition condition, String target, String value) {
        return switch (condition) {
            case STARTS_WITH -> target.startsWith(value);
            case ENDS_WITH -> target.endsWith(value);
            case CONTAINS -> target.contains(value);
            case EQUALS -> target.equals(value);
        };
    }

    /**
     * Helper for enum lookup by string value.
     */
    private interface ValuedEnum {
        String getValue();
    }

    /**
     * Helper method to create a lookup map for valued enums.
     */
    private static <E extends Enum<E> & ValuedEnum> Map<String, E> buildLookup(E[] values) {
        return Arrays.stream(values).collect(Collectors.toMap(v -> v.getValue(), v -> v));
    }

    /**
     * Helper method to lookup an enum by string value.
     */
    private static <E extends Enum<E> & ValuedEnum> E lookup(Map<String, E> lookup, String value, String typeName) {
        E result = lookup.get(value.toLowerCase());
        if (result == null) {
            throw new IllegalArgumentException("Unknown " + typeName + ": " + value);
        }
        return result;
    }

    /**
     * Filter outcome - accept or reject the meter.
     */
    public enum Outcome implements ValuedEnum {
        ACCEPT("accept"),
        REJECT("reject");

        private static final Map<String, Outcome> LOOKUP = buildLookup(values());
        private final String value;

        Outcome(String value) {
            this.value = value;
        }

        @Override
        public String getValue() {
            return value;
        }

        public static Outcome of(String value) {
            return lookup(LOOKUP, value, "outcome");
        }
    }

    /**
     * Filter field - which aspect of the meter to match against.
     */
    public enum Field implements ValuedEnum {
        METER_NAME("meter-name"),
        TAG_NAME("tag-name"),
        TAG_VALUE("tag-value");

        private static final Map<String, Field> LOOKUP = buildLookup(values());
        private final String value;

        Field(String value) {
            this.value = value;
        }

        @Override
        public String getValue() {
            return value;
        }

        public static Field of(String value) {
            return lookup(LOOKUP, value, "field");
        }
    }

    /**
     * Filter condition - how to match the value.
     */
    public enum Condition implements ValuedEnum {
        STARTS_WITH("starts-with"),
        ENDS_WITH("ends-with"),
        CONTAINS("contains"),
        EQUALS("equals");

        private static final Map<String, Condition> LOOKUP = buildLookup(values());
        private final String value;

        Condition(String value) {
            this.value = value;
        }

        @Override
        public String getValue() {
            return value;
        }

        public static Condition of(String value) {
            return lookup(LOOKUP, value, "condition");
        }
    }
}
