/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.wildfly.iiop.openjdk;

/**
 *  @author <a href="mailto:tadamski@redhat.com">Tomasz Adamski</a>
 */

import java.util.HashMap;
import java.util.Map;

enum Namespace {

    UNKNOWN(null),
    IIOP_OPENJDK_1_0("urn:jboss:domain:iiop-openjdk:1.0"),
    IIOP_OPENJDK_2_0("urn:jboss:domain:iiop-openjdk:2.0"),
    IIOP_OPENJDK_2_1("urn:jboss:domain:iiop-openjdk:2.1"),
    IIOP_OPENJDK_3_0("urn:jboss:domain:iiop-openjdk:3.0"),
    IIOP_OPENJDK_3_1("urn:jboss:domain:iiop-openjdk:3.1");

    static final Namespace CURRENT = IIOP_OPENJDK_3_1;


    private final String namespaceURI;

    /**
     * <p>
     * {@code Namespace} constructor. Sets the namespace {@code URI}.
     * </p>
     *
     * @param namespaceURI a {@code String} representing the namespace {@code URI}.
     */
    private Namespace(final String namespaceURI) {
        this.namespaceURI = namespaceURI;
    }

    /**
     * <p>
     * Obtains the {@code URI} of this namespace.
     * </p>
     *
     * @return a {@code String} representing the namespace {@code URI}.
     */
    String getUriString() {
        return namespaceURI;
    }

    // a map that caches all available namespaces by URI.
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

    /**
     * <p>
     * Gets the {@code Namespace} identified by the specified {@code URI}.
     * </p>
     *
     * @param uri a {@code String} representing the namespace {@code URI}.
     * @return the {@code Namespace} identified by the {@code URI}. If no namespace can be found, the {@code Namespace.UNKNOWN}
     *         type is returned.
     */
    static Namespace forUri(final String uri) {
        final Namespace element = MAP.get(uri);
        return element == null ? UNKNOWN : element;
    }
}
