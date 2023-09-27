/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jboss.as.naming.subsystem;

import java.util.HashMap;
import java.util.Map;

/**
 * An enumeration of the supported Naming subsystem namespaces
 *
 * @author Jaikiran Pai
 */
public enum NamingSubsystemNamespace {
    // must be first
    UNKNOWN(null),

    NAMING_1_0("urn:jboss:domain:naming:1.0"),
    NAMING_1_1("urn:jboss:domain:naming:1.1"),
    NAMING_1_2("urn:jboss:domain:naming:1.2"),
    NAMING_1_3("urn:jboss:domain:naming:1.3"),
    NAMING_1_4("urn:jboss:domain:naming:1.4"),
    NAMING_2_0("urn:jboss:domain:naming:2.0"),
    ;


    private final String name;

    NamingSubsystemNamespace(final String name) {
        this.name = name;
    }

    /**
     * Get the URI of this namespace.
     *
     * @return the URI
     */
    public String getUriString() {
        return name;
    }

    private static final Map<String, NamingSubsystemNamespace> MAP;

    static {
        final Map<String, NamingSubsystemNamespace> map = new HashMap<String, NamingSubsystemNamespace>();
        for (NamingSubsystemNamespace namespace : values()) {
            final String name = namespace.getUriString();
            if (name != null) map.put(name, namespace);
        }
        MAP = map;
    }

    public static NamingSubsystemNamespace forUri(String uri) {
        final NamingSubsystemNamespace element = MAP.get(uri);
        return element == null ? UNKNOWN : element;
    }

}
