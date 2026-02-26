/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jboss.as.connector.subsystems.jca;

import java.util.HashMap;
import java.util.Map;

/**
 * A Namespace.
 * @author <a href="mailto:stefano.maestri@redhat.comdhat.com">Stefano
 *         Maestri</a>
 */
public enum Namespace {
    // must be first
    UNKNOWN(null),

    JCA_1_1("urn:jboss:domain:jca:1.1"),

    JCA_2_0("urn:jboss:domain:jca:2.0"),

    JCA_3_0("urn:jboss:domain:jca:3.0"),

    JCA_4_0("urn:jboss:domain:jca:4.0"),

    JCA_5_0("urn:jboss:domain:jca:5.0"),

    JCA_6_0("urn:jboss:domain:jca:6.0"),

    JCA_6_1("urn:jboss:domain:jca:6.1");


    /**
     * The current namespace version.
     */
    public static final Namespace CURRENT = JCA_6_1;

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
