/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2010, Red Hat, Inc., and individual contributors
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

package org.jboss.as.txn.subsystem;

import java.util.HashMap;
import java.util.Map;

/**
 * The namespaces supported by the transactions extension.
 *
 * @author John E. Bailey
 */
enum Namespace {
    // must be first
    UNKNOWN(null),

    TRANSACTIONS_1_0("urn:jboss:domain:transactions:1.0"),
    TRANSACTIONS_1_1("urn:jboss:domain:transactions:1.1"),
    TRANSACTIONS_1_2("urn:jboss:domain:transactions:1.2"),
    TRANSACTIONS_1_3("urn:jboss:domain:transactions:1.3"),
    TRANSACTIONS_1_4("urn:jboss:domain:transactions:1.4"),
    TRANSACTIONS_1_5("urn:jboss:domain:transactions:1.5"),
    TRANSACTIONS_2_0("urn:jboss:domain:transactions:2.0"),
    TRANSACTIONS_3_0("urn:jboss:domain:transactions:3.0"),
    TRANSACTIONS_4_0("urn:jboss:domain:transactions:4.0"),
    TRANSACTIONS_5_0("urn:jboss:domain:transactions:5.0"),
    ;

    /**
     * The current namespace version.
     */
    public static final Namespace CURRENT = TRANSACTIONS_5_0;

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
            if (name != null) map.put(name, namespace);
        }
        MAP = map;
    }

    public static Namespace forUri(String uri) {
        final Namespace element = MAP.get(uri);
        return element == null ? UNKNOWN : element;
    }
}
