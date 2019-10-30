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

package org.jboss.as.ejb3.subsystem;

import java.util.HashMap;
import java.util.Map;

/**
 * An enumeration of the supported EJB3 subsystem namespaces
 *
 * @author Jaikiran Pai
 */
public enum EJB3SubsystemNamespace {
    // must be first
    UNKNOWN(null),

    EJB3_1_0("urn:jboss:domain:ejb3:1.0"),
    EJB3_1_1("urn:jboss:domain:ejb3:1.1"),
    EJB3_1_2("urn:jboss:domain:ejb3:1.2"),
    EJB3_1_3("urn:jboss:domain:ejb3:1.3"),
    EJB3_1_4("urn:jboss:domain:ejb3:1.4"),
    EJB3_1_5("urn:jboss:domain:ejb3:1.5"),
    EJB3_2_0("urn:jboss:domain:ejb3:2.0"),
    EJB3_3_0("urn:jboss:domain:ejb3:3.0"),
    EJB3_4_0("urn:jboss:domain:ejb3:4.0"),
    EJB3_5_0("urn:jboss:domain:ejb3:5.0");


    private final String name;

    EJB3SubsystemNamespace(final String name) {
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

    private static final Map<String, EJB3SubsystemNamespace> MAP;

    static {
        final Map<String, EJB3SubsystemNamespace> map = new HashMap<String, EJB3SubsystemNamespace>();
        for (EJB3SubsystemNamespace namespace : values()) {
            final String name = namespace.getUriString();
            if (name != null) map.put(name, namespace);
        }
        MAP = map;
    }

    public static EJB3SubsystemNamespace forUri(String uri) {
        final EJB3SubsystemNamespace element = MAP.get(uri);
        return element == null ? UNKNOWN : element;
    }

}
