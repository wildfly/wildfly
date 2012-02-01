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
 * The set of valid JGroups protocol types.
 *
 * @author Richard Achmatowicz (c) 2012 Red Hat Inc.
 */
public enum Protocol {
    UNKNOWN(null),
    UDP("UDP"),
    TCP("TCP"),
    TCP_GOSSIP("TCP_GOSSIP"),
    AUTH("AUTH"),
    MPING("MPING"),
    MERGE2("MERGE2"),
    FD_SOCK("FD_SOCK"),
    FD("FD"),
    VERIFY_SUSPECT("VERIFY_SUSPECT"),
    BARRIER("BARRIER"),
    NAKACK("pbcast.NAKACK"),
    UNICAST2("UNICAST2"),
    STABLE("pbcast.STABLE"),
    GMS("pbcast.GMS"),
    UFC("UFC"),
    MFC("MFC"),
    FRAG2("FRAG2"),
    STATE_TRANSFER("pbcast.STATE_TRANSFER"),
    FLUSH("pbcast.FLUSH"),
    ;

    private final String name;

    Protocol(final String name) {
        this.name = name;
    }

    /**
     * Get the local name of this protocol.
     *
     * @return the local name
     */
    public String getLocalName() {
        return name;
    }

    private static final Map<String, Protocol> elements;

    static {
        final Map<String, Protocol> map = new HashMap<String, Protocol>();
        for (Protocol element : values()) {
            final String name = element.getLocalName();
            if (name != null) map.put(name, element);
        }
        elements = map;
    }

    public static Protocol forName(String localName) {
        final Protocol element = elements.get(localName);
        return element == null ? UNKNOWN : element;
    }
}
