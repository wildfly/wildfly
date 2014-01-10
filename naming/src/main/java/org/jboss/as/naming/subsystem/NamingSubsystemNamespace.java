/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2011, Red Hat, Inc., and individual contributors
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

package org.jboss.as.naming.subsystem;

import java.util.HashMap;
import java.util.Map;

/**
 * An enumeration of the supported Naming subsystem namespaces
 *
 * @author Jaikiran Pai
 */
public enum NamingSubsystemNamespace {
    // must be first
    UNKNOWN(null),

    NAMING_1_0("urn:jboss:domain:naming:1.0"),
    NAMING_1_1("urn:jboss:domain:naming:1.1"),
    NAMING_1_2("urn:jboss:domain:naming:1.2"),
    NAMING_1_3("urn:jboss:domain:naming:1.3"),
    NAMING_1_4("urn:jboss:domain:naming:1.4"),
    NAMING_2_0("urn:jboss:domain:naming:2.0"),
    ;


    private final String name;

    NamingSubsystemNamespace(final String name) {
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

    private static final Map<String, NamingSubsystemNamespace> MAP;

    static {
        final Map<String, NamingSubsystemNamespace> map = new HashMap<String, NamingSubsystemNamespace>();
        for (NamingSubsystemNamespace namespace : values()) {
            final String name = namespace.getUriString();
            if (name != null) map.put(name, namespace);
        }
        MAP = map;
    }

    public static NamingSubsystemNamespace forUri(String uri) {
        final NamingSubsystemNamespace element = MAP.get(uri);
        return element == null ? UNKNOWN : element;
    }

}
