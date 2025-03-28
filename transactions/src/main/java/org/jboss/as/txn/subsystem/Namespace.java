/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
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
    TRANSACTIONS_6_0("urn:jboss:domain:transactions:6.0"),
    TRANSACTIONS_6_1("urn:jboss:domain:transactions:6.1"),
    ;

    /**
     * The current namespace version.
     */
    public static final Namespace CURRENT = TRANSACTIONS_6_1;

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
