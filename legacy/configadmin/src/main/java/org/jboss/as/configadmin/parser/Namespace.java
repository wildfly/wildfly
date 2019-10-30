/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2010, Red Hat, Inc., and individual contributors
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

package org.jboss.as.configadmin.parser;

import java.util.EnumSet;
import java.util.HashMap;
import java.util.Map;

/**
 * An enumeration of the supported ConfigAdmin subsystem namespaces.
 *
 * @author Thomas.Diesler@jboss.com
 * @since 13-Sep-2010
 */
enum Namespace {

    // must be first
    UNKNOWN(null),
    VERSION_1_0("urn:jboss:domain:configadmin:1.0")
    ;

    /**
     * The current namespace version.
     */
    static final Namespace CURRENT = VERSION_1_0;

    private final String name;

    Namespace(final String name) {
        this.name = name;
    }

    /**
     * Get the URI of this namespace.
     */
    String getUriString() {
        return name;
    }

    /**
     * Set of all namespaces, excluding the special {@link #UNKNOWN} value.
     */
    static final EnumSet<Namespace> STANDARD_NAMESPACES = EnumSet.complementOf(EnumSet.of(Namespace.UNKNOWN));

    private static final Map<String, Namespace> MAP;

    static {
        final Map<String, Namespace> map = new HashMap<String, Namespace>();
        for (Namespace namespace : values()) {
            final String name = namespace.getUriString();
            if (name != null) map.put(name, namespace);
        }
        MAP = map;
    }

    static Namespace forUri(String uri) {
        final Namespace element = MAP.get(uri);
        return element == null ? UNKNOWN : element;
    }
}
