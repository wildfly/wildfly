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

package org.jboss.as.jacorb;

import java.util.EnumSet;
import java.util.HashMap;
import java.util.Map;

/**
 * <p>
 * Enumeration of the JacORB subsystem XML configuration elements.
 * </p>
 *
 * @author <a href="mailto:sguilhen@redhat.com">Stefan Guilhen</a>
 */
enum JacORBElement {

    UNKNOWN(null),

    // elements used to configure the ORB.
    ORB_CONFIG("orb"),
    ORB_CONNECTION_CONFIG("connection"),
    ORB_NAMING_CONFIG("naming"),

    // elements used to configure the POA.
    POA_CONFIG("poa"),
    POA_REQUEST_PROC_CONFIG("request-processors"),

    // elements used to configure JacORB's interoperability and security.
    INTEROP_CONFIG("interop"),
    SECURITY_CONFIG("security"),

    // element that contains the comma-separated list of ORB initializer aliases.
    INITIALIZERS_CONFIG("initializers"),

    // element used to configure generic properties.
    PROPERTY_CONFIG("property");

    private final String name;

    /**
     * <p>
     * {@code JacORBElement} constructor. Sets the element name.
     * </p>
     *
     * @param name a {@code String} representing the local name of the element.
     */
    JacORBElement(final String name) {
        this.name = name;
    }

    /**
     * <p>
     * Obtains the local name of this element.
     * </p>
     *
     * @return a {@code String} representing the element's local name.
     */
    public String getLocalName() {
        return name;
    }

    // a map that caches all available elements by name.
    private static final Map<String, JacORBElement> MAP;

    static {
        final Map<String, JacORBElement> map = new HashMap<String, JacORBElement>();
        for (JacORBElement element : values()) {
            final String name = element.getLocalName();
            if (name != null)
                map.put(name, element);
        }
        MAP = map;
    }


    /**
     * <p>
     * Gets the {@code JacORBElement} identified by the specified name.
     * </p>
     *
     * @param localName a {@code String} representing the local name of the element.
     * @return the {@code JacORBElement} identified by the name. If no attribute can be found, the
     *         {@code JacORBElement.UNKNOWN} type is returned.
     */
    public static JacORBElement forName(String localName) {
        final JacORBElement element = MAP.get(localName);
        return element == null ? UNKNOWN : element;
    }

    /**
     * <p>
     * Builds and returns an {@code EnumSet} containing all {@code JacORBElement}s that are root elements in the JacORB
     * subsystem configuration.
     * </p>
     * @return
     */
    public static EnumSet<JacORBElement> getRootElements() {
        return EnumSet.of(ORB_CONFIG, POA_CONFIG, INTEROP_CONFIG, SECURITY_CONFIG, PROPERTY_CONFIG, INITIALIZERS_CONFIG);
    }
}
