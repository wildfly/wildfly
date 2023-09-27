/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jboss.as.jpa.subsystem;

import java.util.HashMap;
import java.util.Map;

/**
 * @author Scott Marlow
 */
enum Attribute {

    UNKNOWN(null),
    DEFAULT_DATASOURCE_NAME(CommonAttributes.DEFAULT_DATASOURCE),
    DEFAULT_EXTENDEDPERSISTENCEINHERITANCE_NAME(CommonAttributes.DEFAULT_EXTENDEDPERSISTENCE_INHERITANCE),;
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

    private static final Map<String, Attribute> MAP;

    static {
        final Map<String, Attribute> map = new HashMap<String, Attribute>();
        for (Attribute element : values()) {
            final String name = element.getLocalName();
            if (name != null) map.put(name, element);
        }
        MAP = map;
    }

    public static Attribute forName(String localName) {
        final Attribute element = MAP.get(localName);
        return element == null ? UNKNOWN : element;
    }

    public String toString() {
        return getLocalName();
    }
}
