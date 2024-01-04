/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jboss.as.xts;

import java.util.HashMap;
import java.util.Map;

/**
 * The namespaces supported by the xts extension.
 *
 * @author <a href="mailto:adinn@redhat.com">Andrew Dinn</a>
 */
enum Namespace {
    // must be first
    UNKNOWN(null),

    XTS_1_0("urn:jboss:domain:xts:1.0"),
    XTS_2_0("urn:jboss:domain:xts:2.0"),
    XTS_3_0("urn:jboss:domain:xts:3.0"),
    ;

    /**
     * The current namespace version.
     */
    public static final Namespace CURRENT = XTS_3_0;

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
