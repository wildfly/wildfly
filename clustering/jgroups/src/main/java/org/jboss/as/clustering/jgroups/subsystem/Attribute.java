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
package org.jboss.as.clustering.jgroups.subsystem;

import java.util.HashMap;
import java.util.Map;

import javax.xml.XMLConstants;

public enum Attribute {
    // must be first
    UNKNOWN(null),

    DEFAULT_EXECUTOR(ModelKeys.DEFAULT_EXECUTOR),
    DEFAULT_STACK(ModelKeys.DEFAULT_STACK),
    DIAGNOSTICS_SOCKET_BINDING(ModelKeys.DIAGNOSTICS_SOCKET_BINDING),
    MACHINE(ModelKeys.MACHINE),
    NAME(ModelKeys.NAME),
    NAMESPACE(XMLConstants.XMLNS_ATTRIBUTE),
    OOB_EXECUTOR(ModelKeys.OOB_EXECUTOR),
    RACK(ModelKeys.RACK),
    SHARED(ModelKeys.SHARED),
    SITE(ModelKeys.SITE),
    SOCKET_BINDING(ModelKeys.SOCKET_BINDING),
    THREAD_FACTORY(ModelKeys.THREAD_FACTORY),
    TIMER_EXECUTOR(ModelKeys.TIMER_EXECUTOR),
    TYPE(ModelKeys.TYPE),
    ;

    private final String name;

    private Attribute(final String name) {
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

    private static final Map<String, Attribute> attributes;

    static {
        final Map<String, Attribute> map = new HashMap<String, Attribute>();
        for (Attribute attribute : values()) {
            final String name = attribute.getLocalName();
            if (name != null) map.put(name, attribute);
        }
        attributes = map;
    }

    public static Attribute forName(String localName) {
        final Attribute attribute = attributes.get(localName);
        return attribute == null ? UNKNOWN : attribute;
    }
}
