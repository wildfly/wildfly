/*
 * JBoss, Home of Professional Open Source
 * Copyright 2010, Red Hat Inc., and individual contributors as indicated
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
 * @author Emanuel Muckenhuber
 * @author Jean-Frederic Clere
 */
enum Attribute {
    UNKNOWN(null),
    LOAD_METRIC(CommonAttributes.LOAD_METRIC),
    PROXY_CONF(CommonAttributes.PROXY_CONF),
    HTTPD_CONF(CommonAttributes.HTTPD_CONF),
    NODES_CONF(CommonAttributes.NODES_CONF),
    ADVERTISE_SOCKET(CommonAttributes.ADVERTISE_SOCKET),
    SSL(CommonAttributes.SSL),
    PROXY_LIST(CommonAttributes.PROXY_LIST),
    PROXY_URL(CommonAttributes.PROXY_URL),
    ADVERTISE(CommonAttributes.ADVERTISE),
    ADVERTISE_SECURITY_KEY(CommonAttributes.ADVERTISE_SECURITY_KEY),
    LOAD_PROVIDER(CommonAttributes.LOAD_PROVIDER),
    SIMPLE_LOAD_PROVIDER(CommonAttributes.SIMPLE_LOAD_PROVIDER),
    DYNAMIC_LOAD_PROVIDER(CommonAttributes.DYNAMIC_LOAD_PROVIDER),
    CUSTOM_LOAD_METRIC(CommonAttributes.CUSTOM_LOAD_METRIC),
    EXCLUDED_CONTEXTS(CommonAttributes.EXCLUDED_CONTEXTS),
    AUTO_ENABLE_CONTEXTS(CommonAttributes.AUTO_ENABLE_CONTEXTS),
    STOP_CONTEXT_TIMEOUT(CommonAttributes.STOP_CONTEXT_TIMEOUT),
    SOCKET_TIMEOUT(CommonAttributes.SOCKET_TIMEOUT),
    FACTOR(CommonAttributes.FACTOR),
    HISTORY(CommonAttributes.HISTORY),
    DECAY(CommonAttributes.DECAY),
    NAME(CommonAttributes.NAME),
    CAPACITY(CommonAttributes.CAPACITY),
    WEIGHT(CommonAttributes.WEIGHT),
    TYPE(CommonAttributes.TYPE),
    CLASS(CommonAttributes.CLASS),
    ;

    private final String name;

    Attribute(final String name) {
        this.name = name;
    }

    /**
     * Get the local name of this attribute.
     *
     * @return the local name
     */
    public String getLocalName() {
        return name;
    }

    private static final Map<String, Attribute> MAP;

    static {
        final Map<String, Attribute> map = new HashMap<String, Attribute>();
        for (Attribute element : values()) {
            final String name = element.getLocalName();
            if (name != null)
                map.put(name, element);
        }
        MAP = map;
    }

    public static Attribute forName(String localName) {
        final Attribute element = MAP.get(localName);
        return element == null ? UNKNOWN : element;
    }

    @Override
    public String toString() {
        return getLocalName();
    }
}
