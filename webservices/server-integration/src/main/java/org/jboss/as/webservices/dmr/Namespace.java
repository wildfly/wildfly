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
package org.jboss.as.webservices.dmr;

import java.util.HashMap;
import java.util.Map;

/**
 * @author alessio.soldano@jboss.com
 * @author <a href="mailto:ropalka@redhat.com">Richard Opalka</a>
 * @author <a href="mailto:ema@redhat.com">Jim Ma</a>
 */
enum Namespace {

    // must be first
    UNKNOWN(null),

    WEBSERVICES_1_0("urn:jboss:domain:webservices:1.0"),

    WEBSERVICES_1_1("urn:jboss:domain:webservices:1.1"),

    WEBSERVICES_1_2("urn:jboss:domain:webservices:1.2"),

    WEBSERVICES_2_0("urn:jboss:domain:webservices:2.0"),

    JAVAEE("http://java.sun.com/xml/ns/javaee"),

    JAVAEE_7_0("http://xmlns.jcp.org/xml/ns/javaee"),

    JAXWSCONFIG("urn:jboss:jbossws-jaxws-config:4.0");

    /**
     * The current namespace version.
     */
    static final Namespace CURRENT = WEBSERVICES_2_0;

    private final String name;

    private Namespace(final String name) {
        this.name = name;
    }

    private static final Map<String, Namespace> MAP;

    static {
        final Map<String, Namespace> map = new HashMap<String, Namespace>();
        for (final Namespace namespace : values()) {
            final String name = namespace.getUriString();
            if (name != null)
                map.put(name, namespace);
        }
        MAP = map;
    }

    static Namespace forUri(final String uri) {
        final Namespace element = MAP.get(uri);
        return element == null ? UNKNOWN : element;
    }

    /**
     * Get the URI of this namespace.
     *
     * @return the URI
     */
    String getUriString() {
        return name;
    }

}
