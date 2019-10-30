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
 * Enumeration of elements used in the Naming subsystem
 *
 * @author Stuart Douglas
 */
public enum NamingSubsystemXMLElement {

    // must be first
    UNKNOWN(null),

    BINDINGS("bindings"),
    REMOTE_NAMING("remote-naming"),
    SIMPLE("simple"),

    LOOKUP("lookup"),

    OBJECT_FACTORY("object-factory"),
    ENVIRONMENT(NamingSubsystemModel.ENVIRONMENT),
    ENVIRONMENT_PROPERTY("property"),

    EXTERNAL_CONTEXT("external-context"),
    ;

    private final String name;

    NamingSubsystemXMLElement(final String name) {
        this.name = name;
    }

    /**
     * Get the local name of this element.
     *
     * @return the local name
     */
    public String getLocalName() {
        return name;
    }

    private static final Map<String, NamingSubsystemXMLElement> MAP;

    static {
        final Map<String, NamingSubsystemXMLElement> map = new HashMap<String, NamingSubsystemXMLElement>();
        for (NamingSubsystemXMLElement element : values()) {
            final String name = element.getLocalName();
            if (name != null) map.put(name, element);
        }
        MAP = map;
    }

    public static NamingSubsystemXMLElement forName(String localName) {
        final NamingSubsystemXMLElement element = MAP.get(localName);
        return element == null ? UNKNOWN : element;
    }
}
