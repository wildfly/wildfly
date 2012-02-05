/*
 * JBoss, Home of Professional Open Source
 * Copyright 2012, Red Hat Inc., and individual contributors as indicated
 * by the @authors tag. See the copyright.txt in the distribution for a
 * full listing of individual contributors.
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
package org.jboss.as.modcluster;

import java.util.HashMap;
import java.util.Map;

/**
 * @author Jean-Frederic Clere
 * @author Radoslav Husar
 */
enum Element {
    // must be first
    UNKNOWN(null),
    MOD_CLUSTER_CONFIG(CommonAttributes.MOD_CLUSTER_CONFIG),
    LOAD_PROVIDER(CommonAttributes.LOAD_PROVIDER),
    PROXY_CONF(CommonAttributes.PROXY_CONF),
    HTTPD_CONF(CommonAttributes.HTTPD_CONF),
    NODES_CONF(CommonAttributes.NODES_CONF),
    ADVERTISE_SOCKET(CommonAttributes.ADVERTISE_SOCKET),
    SSL(CommonAttributes.SSL),
    DYNAMIC_LOAD_PROVIDER(CommonAttributes.DYNAMIC_LOAD_PROVIDER),
    SIMPLE_LOAD_PROVIDER(CommonAttributes.SIMPLE_LOAD_PROVIDER),
    LOAD_METRIC(CommonAttributes.LOAD_METRIC),
    CUSTOM_LOAD_METRIC(CommonAttributes.CUSTOM_LOAD_METRIC),
    PROPERTY(CommonAttributes.PROPERTY),

    /**
     * @since v1.1
     */
    STICKY_SESSION(CommonAttributes.STICKY_SESSION),
    ADVERTISE(CommonAttributes.ADVERTISE),
    PROXIES(CommonAttributes.PROXIES),
    CONTEXTS(CommonAttributes.CONTEXTS),
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

    private static final Map<String, Element> MAP;

    static {
        final Map<String, Element> map = new HashMap<String, Element>();
        for (Element element : values()) {
            final String name = element.getLocalName();
            if (name != null)
                map.put(name, element);
        }
        MAP = map;
    }

    public static Element forName(String localName) {
        final Element element = MAP.get(localName);
        return element == null ? UNKNOWN : element;
    }

}
