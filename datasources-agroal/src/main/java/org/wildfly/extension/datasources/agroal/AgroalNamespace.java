/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2017, Red Hat, Inc., and individual contributors
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

    AGROAL_1_0("urn:jboss:domain:datasources-agroal:1.0");

    public static final AgroalNamespace CURRENT = AGROAL_1_0;

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
