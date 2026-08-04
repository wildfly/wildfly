/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jboss.as.connector.subsystems.datasources;

import java.util.HashMap;
import java.util.Map;

/**
 * A Namespace.
 * @author <a href="mailto:stefano.maestri@redhat.comdhat.com">Stefano Maestri</a>
 */
public enum Namespace {
    // must be first
    UNKNOWN(null),

    DATASOURCES_1_1("urn:jboss:domain:datasources:1.1"),

    DATASOURCES_1_2("urn:jboss:domain:datasources:1.2"),

    DATASOURCES_2_0("urn:jboss:domain:datasources:2.0"),

    DATASOURCES_3_0("urn:jboss:domain:datasources:3.0"),

    DATASOURCES_4_0("urn:jboss:domain:datasources:4.0"),

    DATASOURCES_5_0("urn:jboss:domain:datasources:5.0"),

    DATASOURCES_6_0("urn:jboss:domain:datasources:6.0"),

    DATASOURCES_7_0("urn:jboss:domain:datasources:7.0"),

    DATASOURCES_7_1("urn:jboss:domain:datasources:7.1"),

    DATASOURCES_7_2("urn:jboss:domain:datasources:7.2"),

    DATASOURCES_8_0("urn:jboss:domain:datasources:8.0");
    /**
     * The current namespace version.
     */
    public static final Namespace CURRENT = DATASOURCES_8_0;

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
