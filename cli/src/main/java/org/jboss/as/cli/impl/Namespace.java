/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2012, Red Hat, Inc., and individual contributors
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

package org.jboss.as.cli.impl;

import java.util.HashMap;
import java.util.Map;

import javax.xml.XMLConstants;

/**
 * An enumeration of the supported namespaces for CLI configuration.
 *
 * @author <a href="mailto:darran.lofthouse@jboss.com">Darran Lofthouse</a>
 */
public enum Namespace {

    // must be first
    UNKNOWN(null), NONE(null),

    // predefined standard
    XML_SCHEMA_INSTANCE("http://www.w3.org/2001/XMLSchema-instance"),

    CLI_1_0("urn:jboss:cli:1.0"),

    CLI_1_1("urn:jboss:cli:1.1"),

    CLI_1_2("urn:jboss:cli:1.2");

    /**
     * The current namespace version.
     */
    public static final Namespace CURRENT = CLI_1_2;

    private final String name;

    Namespace(final String name) {
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

    private static final Map<String, Namespace> MAP;

    static {
        final Map<String, Namespace> map = new HashMap<String, Namespace>();
        for (Namespace namespace : values()) {
            final String name = namespace.getUriString();
            if (name != null)
                map.put(name, namespace);
        }
        MAP = map;
    }

    public static Namespace forUri(String uri) {
        // FIXME when STXM-8 is done, remove the null check
        if (uri == null || XMLConstants.NULL_NS_URI.equals(uri))
            return NONE;
        final Namespace element = MAP.get(uri);
        return element == null ? UNKNOWN : element;
    }

    public static Namespace[] cliValues() {
        Namespace[] temp = values();
        // The 3 is for the 3 namespaces excluded below.
        Namespace[] response = new Namespace[temp.length - 3];
        int nextPos = 0;
        for (Namespace current : temp) {
            if (current != UNKNOWN && current != NONE && current != XML_SCHEMA_INSTANCE) {
                response[nextPos++] = current;
            }
        }

        return response;
    }

}
