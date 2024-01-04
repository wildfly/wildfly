/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jboss.as.connector.subsystems.datasources;

import java.util.HashMap;
import java.util.Map;

/**
 * An Element.
 * @author <a href="mailto:stefano.maestri@redhat.com">Stefano Maestri</a>
 */

public enum Element {
    /** always the first **/
    UNKNOWN(null), SUBSYSTEM("subsystem"), DATASOURCES("datasources"),  DRIVERS("drivers"), DRIVER("driver");

    private final String name;

    Element(final String name) {
        this.name = name;
    }

    /**
     * Get the local name of this element.
     * @return the local name
     */
    public String getLocalName() {
        return name;
    }

    private static final Map<String, Element> MAP;

    static {
        final Map<String, Element> map = new HashMap<String, Element>();
        for (Element element : values()) {
            final String name = element.getLocalName();
            if (name != null)
                map.put(name, element);
        }
        MAP = map;
    }

    public static Element forName(String localName) {
        final Element element = MAP.get(localName);
        return element == null ? UNKNOWN : element;
    }
}
