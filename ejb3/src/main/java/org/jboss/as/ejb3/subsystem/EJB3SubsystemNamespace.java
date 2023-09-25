/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jboss.as.ejb3.subsystem;

import java.util.HashMap;
import java.util.Map;

/**
 * An enumeration of the supported EJB3 subsystem namespaces
 *
 * @author Jaikiran Pai
 */
public enum EJB3SubsystemNamespace {
    // must be first
    UNKNOWN(null),

    EJB3_1_0("urn:jboss:domain:ejb3:1.0"),
    EJB3_1_1("urn:jboss:domain:ejb3:1.1"),
    EJB3_1_2("urn:jboss:domain:ejb3:1.2"),
    EJB3_1_3("urn:jboss:domain:ejb3:1.3"),
    EJB3_1_4("urn:jboss:domain:ejb3:1.4"),
    EJB3_1_5("urn:jboss:domain:ejb3:1.5"),
    EJB3_2_0("urn:jboss:domain:ejb3:2.0"),
    EJB3_3_0("urn:jboss:domain:ejb3:3.0"),
    EJB3_4_0("urn:jboss:domain:ejb3:4.0"),
    EJB3_5_0("urn:jboss:domain:ejb3:5.0"),
    EJB3_6_0("urn:jboss:domain:ejb3:6.0"),
    EJB3_7_0("urn:jboss:domain:ejb3:7.0"),
    EJB3_8_0("urn:jboss:domain:ejb3:8.0"),
    EJB3_9_0("urn:jboss:domain:ejb3:9.0"),
    EJB3_10_0("urn:jboss:domain:ejb3:10.0");


    private final String name;

    EJB3SubsystemNamespace(final String name) {
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

    private static final Map<String, EJB3SubsystemNamespace> MAP;

    static {
        final Map<String, EJB3SubsystemNamespace> map = new HashMap<String, EJB3SubsystemNamespace>();
        for (EJB3SubsystemNamespace namespace : values()) {
            final String name = namespace.getUriString();
            if (name != null) map.put(name, namespace);
        }
        MAP = map;
    }

    public static EJB3SubsystemNamespace forUri(String uri) {
        final EJB3SubsystemNamespace element = MAP.get(uri);
        return element == null ? UNKNOWN : element;
    }

}
