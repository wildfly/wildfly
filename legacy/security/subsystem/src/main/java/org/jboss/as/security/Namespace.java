/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jboss.as.security;

import java.util.HashMap;
import java.util.Map;

/**
 * An enumeration of the supported Security subsystem namespaces
 *
 * @author <a href="mailto:mmoyses@redhat.com">Marcus Moyses</a>
 */
public enum Namespace {
    // must be first
    UNKNOWN(null),

    SECURITY_1_0("urn:jboss:domain:security:1.0"),
    SECURITY_1_1("urn:jboss:domain:security:1.1"),
    SECURITY_1_2("urn:jboss:domain:security:1.2"),
    SECURITY_2_0("urn:jboss:domain:security:2.0");

    /**
     * The current namespace version.
     */
    public static final Namespace CURRENT = SECURITY_2_0;

    private final String name;

    Namespace(final String name) {
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

    private static final Map<String, Namespace> MAP;

    static {
        final Map<String, Namespace> map = new HashMap<String, Namespace>();
        for (Namespace namespace : values()) {
            final String name = namespace.getUriString();
            if (name != null) map.put(name, namespace);
        }
        MAP = map;
    }

    public static Namespace forUri(String uri) {
        final Namespace element = MAP.get(uri);
        return element == null ? UNKNOWN : element;
    }

}
