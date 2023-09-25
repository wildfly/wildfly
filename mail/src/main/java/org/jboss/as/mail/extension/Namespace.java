/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jboss.as.mail.extension;

import java.util.HashMap;
import java.util.Map;

/**
 * @author <a href="tomaz.cerar@gmail.com">Tomaz Cerar</a>
 */
enum Namespace {
    // must be first
    UNKNOWN(null),

    MAIL_1_0("urn:jboss:domain:mail:1.0"),
    MAIL_1_1("urn:jboss:domain:mail:1.1"),
    MAIL_1_2("urn:jboss:domain:mail:1.2"),
    MAIL_2_0("urn:jboss:domain:mail:2.0"),
    MAIL_3_0("urn:jboss:domain:mail:3.0"),
    MAIL_4_0("urn:jboss:domain:mail:4.0");

    /**
     * The current namespace version.
     */
    public static final Namespace CURRENT = MAIL_4_0;

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
            if (name != null) { map.put(name, namespace); }
        }
        MAP = map;
    }

    public static Namespace forUri(String uri) {
        final Namespace element = MAP.get(uri);
        return element == null ? UNKNOWN : element;
    }
}
