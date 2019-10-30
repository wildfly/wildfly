package org.wildfly.iiop.openjdk;

/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2016, Red Hat, Inc., and individual contributors
 * as indicated by the @author tags. See the copyright.txt file in the
 * distribution for a full listing of individual contributors.
 *
 * This is free software; you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation; either version 2.1 of
 * the License, or (at your option) any later version.
 *
 * This software is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this software; if not, write to the Free
 * Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA
 * 02110-1301 USA, or see the FSF site: http://www.fsf.org.
 */

/**
 *  @author <a href="mailto:tadamski@redhat.com">Tomasz Adamski</a>
 */

import java.util.HashMap;
import java.util.Map;

enum Namespace {

    UNKNOWN(null),
    IIOP_OPENJDK_1_0("urn:jboss:domain:iiop-openjdk:1.0"),
    IIOP_OPENJDK_2_0("urn:jboss:domain:iiop-openjdk:2.0"),
    IIOP_OPENJDK_2_1("urn:jboss:domain:iiop-openjdk:2.1");

    static final Namespace CURRENT = IIOP_OPENJDK_2_1;


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
