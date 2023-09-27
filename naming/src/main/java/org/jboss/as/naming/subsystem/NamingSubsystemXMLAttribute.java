/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.jboss.as.naming.subsystem;

import java.util.HashMap;
import java.util.Map;

/**
 * @author Stuart Douglas
 */
public enum NamingSubsystemXMLAttribute {
    UNKNOWN(null),

    CACHE("cache"),
    CLASS("class"),
    LOOKUP("lookup"),
    MODULE("module"),
    NAME("name"),
    TYPE("type"),
    VALUE("value"),;

    private final String name;

    NamingSubsystemXMLAttribute(final String name) {
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

    private static final Map<String, NamingSubsystemXMLAttribute> MAP;

    static {
        final Map<String, NamingSubsystemXMLAttribute> map = new HashMap<String, NamingSubsystemXMLAttribute>();
        for (NamingSubsystemXMLAttribute element : values()) {
            final String name = element.getLocalName();
            if (name != null)
                map.put(name, element);
        }
        MAP = map;
    }

    public static NamingSubsystemXMLAttribute forName(String localName) {
        final NamingSubsystemXMLAttribute element = MAP.get(localName);
        return element == null ? UNKNOWN : element;
    }

    @Override
    public String toString() {
        return getLocalName();
    }
}
