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

package org.jboss.as.clustering.infinispan.subsystem;

import java.util.HashMap;
import java.util.Map;

/**
 * @author Paul Ferraro
 */
public enum Element {
    // must be first
    UNKNOWN(null),

    ALIAS(ModelKeys.ALIAS),
    CACHE_CONTAINER(ModelKeys.CACHE_CONTAINER),
    DISTRIBUTED_CACHE(ModelKeys.DISTRIBUTED_CACHE),
    EVICTION(ModelKeys.EVICTION),
    EXPIRATION(ModelKeys.EXPIRATION),
    FILE_STORE(ModelKeys.FILE_STORE),
    INVALIDATION_CACHE(ModelKeys.INVALIDATION_CACHE),
    LOCAL_CACHE(ModelKeys.LOCAL_CACHE),
    LOCKING(ModelKeys.LOCKING),
    PROPERTY(ModelKeys.PROPERTY),
    REHASHING(ModelKeys.REHASHING),
    REPLICATED_CACHE(ModelKeys.REPLICATED_CACHE),
    STATE_TRANSFER(ModelKeys.STATE_TRANSFER),
    STORE(ModelKeys.STORE),
    SUBSYSTEM(org.jboss.as.controller.parsing.Element.SUBSYSTEM.getLocalName()),
    TRANSACTION(ModelKeys.TRANSACTION),
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
        return name;
    }

    private static final Map<String, Element> elements;

    static {
        final Map<String, Element> map = new HashMap<String, Element>();
        for (Element element : values()) {
            final String name = element.getLocalName();
            if (name != null) map.put(name, element);
        }
        elements = map;
    }

    public static Element forName(String localName) {
        final Element element = elements.get(localName);
        return element == null ? UNKNOWN : element;
    }
}
