/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.wildfly.extension.datasources.agroal;

import java.util.HashMap;
import java.util.Map;

/**
 * Namespace definitions for the XML parser
 *
 * @author <a href="lbarreiro@redhat.com">Luis Barreiro</a>
 */
enum AgroalNamespace {

    UNKNOWN(null), // must be first

    AGROAL_1_0("urn:jboss:domain:datasources-agroal:1.0"),

    AGROAL_2_0("urn:jboss:domain:datasources-agroal:2.0");

    public static final AgroalNamespace CURRENT = AGROAL_2_0;

    private static final Map<String, AgroalNamespace> MAP;

    static {
        Map<String, AgroalNamespace> map = new HashMap<>();
        for (AgroalNamespace namespace : values()) {
            final String name = namespace.getUriString();
            if (name != null) {
                map.put(name, namespace);
            }
        }
        MAP = map;
    }

    // --- //

    private final String name;

    AgroalNamespace(final String name) {
        this.name = name;
    }

    /**
     * Get the Namespace instance for a given URI
     */
    @SuppressWarnings("unused")
    public static AgroalNamespace forUri(String uri) {
        AgroalNamespace element = MAP.get(uri);
        return element == null ? UNKNOWN : element;
    }

    /**
     * Get the URI of this AgroalNamespace
     */
    public String getUriString() {
        return name;
    }
}
