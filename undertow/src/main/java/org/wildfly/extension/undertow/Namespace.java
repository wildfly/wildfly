/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2017, Red Hat, Inc., and individual contributors
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

package org.wildfly.extension.undertow;

import java.util.HashMap;
import java.util.Map;

/**
 * @author <a href="mailto:tomaz.cerar@redhat.com">Tomaz Cerar</a> (c) 2012 Red Hat Inc.
 */
enum Namespace {

    // must be first
    UNKNOWN(null),

    UNDERTOW_1_0("urn:jboss:domain:undertow:1.0"),
    UNDERTOW_1_1("urn:jboss:domain:undertow:1.1"),
    UNDERTOW_1_2("urn:jboss:domain:undertow:1.2"),
    UNDERTOW_2_0("urn:jboss:domain:undertow:2.0"),
    UNDERTOW_3_0("urn:jboss:domain:undertow:3.0"),
    UNDERTOW_3_1("urn:jboss:domain:undertow:3.1"),
    UNDERTOW_4_0("urn:jboss:domain:undertow:4.0"),
    UNDERTOW_5_0("urn:jboss:domain:undertow:5.0"),
    UNDERTOW_6_0("urn:jboss:domain:undertow:6.0"),
    UNDERTOW_7_0("urn:jboss:domain:undertow:7.0");

    /**
     * The current namespace version.
     */
    public static final Namespace CURRENT = UNDERTOW_7_0;

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
            if (name != null) { map.put(name, namespace); }
        }
        MAP = map;
    }

    public static Namespace forUri(String uri) {
        final Namespace element = MAP.get(uri);
        return element == null ? UNKNOWN : element;
    }

}
