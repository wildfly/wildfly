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

/**
 * @author <a href="mailto:david.lloyd@redhat.com">David M. Lloyd</a>
 */
public enum Element {
    // must be first
    UNKNOWN(null),

    CHANNEL(ModelKeys.CHANNEL),
    CHANNELS("channels"),
    FORK(ModelKeys.FORK),
    PROPERTY(ModelKeys.PROPERTY),
    PROTOCOL(ModelKeys.PROTOCOL),
    RELAY(ModelKeys.RELAY),
    REMOTE_SITE(ModelKeys.REMOTE_SITE),
    STACK(ModelKeys.STACK),
    STACKS("stacks"),
    TRANSPORT(ModelKeys.TRANSPORT),
    ;

    private final String name;

    Element(final String name) {
        this.name = name;
    }

    /**
     * Get the local name of this element.
     *
     * @return the local name
     */
    public String getLocalName() {
        return this.name;
    }

    private static final Map<String, Element> elements = new HashMap<>();

    static {
        for (Element element : values()) {
            String name = element.getLocalName();
            if (name != null) {
                elements.put(name, element);
            }
        }
    }

    public static Element forName(String localName) {
        Element element = elements.get(localName);
        return (element != null) ? element : UNKNOWN;
    }
}
