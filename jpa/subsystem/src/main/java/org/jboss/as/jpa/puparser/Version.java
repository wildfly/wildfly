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

package org.jboss.as.jpa.puparser;

import java.util.HashMap;
import java.util.Map;

/**
 * @author Scott Marlow
 */
public enum Version {

    UNKNOWN(null),
    JPA_1_0("http://java.sun.com/xml/ns/persistence/persistence_1_0.xsd"),
    JPA_2_0("http://java.sun.com/xml/ns/persistence/persistence_2_0.xsd"),
    JPA_2_1("http://xmlns.jcp.org/xml/ns/persistence/persistence_2_1.xsd"),
    JPA_2_2("http://xmlns.jcp.org/xml/ns/persistence/persistence_2_2.xsd");


    private static final Map<String, Version> bindings = new HashMap<String, Version>();

    private final String location;

    Version(String location) {
        this.location = location;
    }

    static {
        for (Version version : values()) {
            bindings.put(version.location, version);
        }
    }

    public static Version forLocation(final String location) {
        final Version version = bindings.get(location);
        return version != null ? version : UNKNOWN;
    }
}
