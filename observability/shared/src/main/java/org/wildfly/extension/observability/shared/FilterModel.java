/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.wildfly.extension.observability.shared;

import java.util.Arrays;
import java.util.Map;
import java.util.stream.Collectors;

import org.jboss.dmr.ModelNode;

public record FilterModel(Outcome outcome, Field field, Condition condition, boolean negate, String value) {

    public static FilterModel of(ModelNode model) {
        return new FilterModel(Outcome.of(model.get("outcome").asString("reject")),
                Field.of(model.get("field").asString()),
                Condition.of(model.get("condition").asString()),
                model.get("negate").asBoolean(false),
                model.get("value").asString());
    }

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

    static boolean matchesCondition(Condition condition, String target, String value) {
        return switch (condition) {
            case STARTS_WITH -> target.startsWith(value);
            case ENDS_WITH -> target.endsWith(value);
            case CONTAINS -> target.contains(value);
            case EQUALS -> target.equals(value);
        };
    }

    public enum Outcome {
        ACCEPT("accept"),
        REJECT("reject");

        private static final Map<String, Outcome> LOOKUP =
                Arrays.stream(values()).collect(Collectors.toMap(c -> c.value, c -> c));

        private final String value;

        Outcome(String value) {
            this.value = value;
        }

        public String getValue() {
            return value;
        }

        public static Outcome of(String value) {
            Outcome c = LOOKUP.get(value.toLowerCase());
            if (c == null) {
                throw new IllegalArgumentException("Unknown outcome: " + value);
            }
            return c;
        }
    }

    public enum Field {
        METER_NAME("meter-name"),
        TAG_NAME("tag-name"),
        TAG_VALUE("tag-value");

        private static final Map<String, Field> LOOKUP =
                Arrays.stream(values()).collect(Collectors.toMap(c -> c.value, c -> c));

        private final String value;

        Field(String value) {
            this.value = value;
        }

        public String getValue() {
            return value;
        }

        public static Field of(String value) {
            Field c = LOOKUP.get(value.toLowerCase());
            if (c == null) {
                throw new IllegalArgumentException("Unknown field: " + value);
            }
            return c;
        }
    }

    public enum Condition {
        STARTS_WITH("starts-with"),
        ENDS_WITH("ends-with"),
        CONTAINS("contains"),
        EQUALS("equals");

        private static final Map<String, Condition> LOOKUP =
                Arrays.stream(values()).collect(Collectors.toMap(c -> c.value, c -> c));

        private final String value;

        Condition(String value) {
            this.value = value;
        }

        public String getValue() {
            return value;
        }

        public static Condition of(String value) {
            Condition c = LOOKUP.get(value.toLowerCase());
            if (c == null) {
                throw new IllegalArgumentException("Unknown condition: " + value);
            }
            return c;
        }
    }
}
