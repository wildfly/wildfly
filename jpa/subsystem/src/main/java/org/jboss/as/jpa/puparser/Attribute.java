/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jboss.as.jpa.puparser;

import java.util.HashMap;
import java.util.Map;

/**
 * @author Scott Marlow (cloned from Remy Maucherat)
 */
public enum Attribute {
    // always first
    UNKNOWN(null),

    // attributes in alpha order
    ID("id"),
    LANG("lang"),
    NAME("name"),
    TRANSACTIONTYPE("transaction-type"),
    VALUE("value"),
    VERSION("version");

    private final String name;

    Attribute(final String name) {
        this.name = name;
    }

    /**
     * Get the local name of this element.
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
}
