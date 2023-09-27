/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.jboss.as.webservices.dmr;

import java.util.HashMap;
import java.util.Map;

/**
 * @author alessio.soldano@jboss.com
 * @author <a href="mailto:ropalka@redhat.com">Richard Opalka</a>
 * @author <a href="mailto:ema@redhat.com">Jim Ma</a>
 */
enum Namespace {

    // must be first
    UNKNOWN(null),

    WEBSERVICES_1_0("urn:jboss:domain:webservices:1.0"),

    WEBSERVICES_1_1("urn:jboss:domain:webservices:1.1"),

    WEBSERVICES_1_2("urn:jboss:domain:webservices:1.2"),

    WEBSERVICES_2_0("urn:jboss:domain:webservices:2.0"),

    JAVAEE("http://java.sun.com/xml/ns/javaee"),

    JAVAEE_7_0("http://xmlns.jcp.org/xml/ns/javaee"),

    JAXWSCONFIG("urn:jboss:jbossws-jaxws-config:4.0");

    /**
     * The current namespace version.
     */
    static final Namespace CURRENT = WEBSERVICES_2_0;

    private final String name;

    private Namespace(final String name) {
        this.name = name;
    }

    private static final Map<String, Namespace> MAP;

    static {
        final Map<String, Namespace> map = new HashMap<String, Namespace>();
        for (final Namespace namespace : values()) {
            final String name = namespace.getUriString();
            if (name != null)
                map.put(name, namespace);
        }
        MAP = map;
    }

    static Namespace forUri(final String uri) {
        final Namespace element = MAP.get(uri);
        return element == null ? UNKNOWN : element;
    }

    /**
     * Get the URI of this namespace.
     *
     * @return the URI
     */
    String getUriString() {
        return name;
    }

}
