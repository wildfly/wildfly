/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jipijapa.cache.spi;

import java.util.HashMap;
import java.util.Map;

/**
 * Type of cache
 *
 * @author Scott Marlow
 */
public enum Classification {
    INFINISPAN("Infinispan"),
    NONE(null);

    private final String name;

    Classification(final String name) {
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

    private static final Map<String, Classification> MAP;

    static {
        final Map<String, Classification> map = new HashMap<String, Classification>();
        for (Classification element : values()) {
            final String name = element.getLocalName();
            if (name != null) map.put(name, element);
        }
        MAP = map;
    }

    public static Classification forName(String localName) {
        final Classification element = MAP.get(localName);
        return element == null ? NONE : element;
    }

}
