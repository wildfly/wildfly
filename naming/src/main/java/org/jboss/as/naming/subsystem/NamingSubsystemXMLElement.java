/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.jboss.as.naming.subsystem;

import java.util.HashMap;
import java.util.Map;

/**
 * Enumeration of elements used in the Naming subsystem
 *
 * @author Stuart Douglas
 */
public enum NamingSubsystemXMLElement {

    // must be first
    UNKNOWN(null),

    BINDINGS("bindings"),
    REMOTE_NAMING("remote-naming"),
    SIMPLE("simple"),

    LOOKUP("lookup"),

    OBJECT_FACTORY("object-factory"),
    ENVIRONMENT(NamingSubsystemModel.ENVIRONMENT),
    ENVIRONMENT_PROPERTY("property"),

    EXTERNAL_CONTEXT("external-context"),
    ;

    private final String name;

    NamingSubsystemXMLElement(final String name) {
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

    private static final Map<String, NamingSubsystemXMLElement> MAP;

    static {
        final Map<String, NamingSubsystemXMLElement> map = new HashMap<String, NamingSubsystemXMLElement>();
        for (NamingSubsystemXMLElement element : values()) {
            final String name = element.getLocalName();
            if (name != null) map.put(name, element);
        }
        MAP = map;
    }

    public static NamingSubsystemXMLElement forName(String localName) {
        final NamingSubsystemXMLElement element = MAP.get(localName);
        return element == null ? UNKNOWN : element;
    }
}
