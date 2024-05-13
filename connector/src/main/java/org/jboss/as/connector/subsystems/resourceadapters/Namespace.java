/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jboss.as.connector.subsystems.resourceadapters;

import java.util.HashMap;
import java.util.Map;

/**
 * A Namespace.
 * @author <a href="mailto:stefano.maestri@redhat.comdhat.com">Stefano Maestri</a>
 */
public enum Namespace {
    // must be first
    UNKNOWN(null),

    RESOURCEADAPTERS_1_0("urn:jboss:domain:resource-adapters:1.0"),

    RESOURCEADAPTERS_1_1("urn:jboss:domain:resource-adapters:1.1"),

    RESOURCEADAPTERS_2_0("urn:jboss:domain:resource-adapters:2.0"),

    RESOURCEADAPTERS_3_0("urn:jboss:domain:resource-adapters:3.0"),

    RESOURCEADAPTERS_4_0("urn:jboss:domain:resource-adapters:4.0"),

    RESOURCEADAPTERS_5_0("urn:jboss:domain:resource-adapters:5.0"),

    RESOURCEADAPTERS_6_0("urn:jboss:domain:resource-adapters:6.0"),

    RESOURCEADAPTERS_6_1("urn:jboss:domain:resource-adapters:6.1"),

    RESOURCEADAPTERS_7_0("urn:jboss:domain:resource-adapters:7.0"),

    RESOURCEADAPTERS_7_1("urn:jboss:domain:resource-adapters:7.1"),

    RESOURCEADAPTERS_7_2("urn:jboss:domain:resource-adapters:7.2");

    /**
     * The current namespace version.
     */
    public static final Namespace CURRENT = RESOURCEADAPTERS_7_2;

    private final String name;

    Namespace(final String name) {
        this.name = name;
    }

    /**
     * Get the URI of this namespace.
     * @return the URI
     */
    public String getUriString() {
        return name;
    }

    private static final Map<String, Namespace> MAP;

    static {
        final Map<String, Namespace> map = new HashMap<String, Namespace>();
        for (Namespace namespace : values()) {
            final String name = namespace.getUriString();
            if (name != null)
                map.put(name, namespace);
        }
        MAP = map;
    }

    public static Namespace forUri(String uri) {
        final Namespace element = MAP.get(uri);
        return element == null ? UNKNOWN : element;
    }
}
