/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.wildfly.extension.batch.jberet;

import java.util.Map;

/**
 * @author <a href="mailto:jperkins@redhat.com">James R. Perkins</a>
 */
public enum Attribute {
    UNKNOWN(null),
    DATA_SOURCE("data-source"),
    NAME("name"),
    VALUE("value"),
    EXECUTION_RECORDS_LIMIT("execution-records-limit");

    private static final Map<String, Attribute> MAP = Map.of(
            DATA_SOURCE.name, DATA_SOURCE,
            NAME.name, NAME,
            VALUE.name, VALUE,
            EXECUTION_RECORDS_LIMIT.name, EXECUTION_RECORDS_LIMIT);

    private final String name;

    Attribute(final String name) {
        this.name = name;
    }

    /**
     * Get the local name of this attribute.
     *
     * @return the local name
     */
    public String getLocalName() {
        return name;
    }

    public static Attribute forName(String localName) {
        if (localName == null) {
            return UNKNOWN;
        }
        final Attribute element = MAP.get(localName);
        return element == null ? UNKNOWN : element;
    }
}
