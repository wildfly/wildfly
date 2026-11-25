/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jboss.as.jpa.puparser;

import java.util.HashMap;
import java.util.Map;

/**
 * @author Scott Marlow
 */
public enum Version {

    UNKNOWN(null, null),
    JPA_1_0("http://java.sun.com/xml/ns/persistence/persistence_1_0.xsd", "1.0"),
    JPA_2_0("http://java.sun.com/xml/ns/persistence/persistence_2_0.xsd", "2.0"),
    JPA_2_1("http://xmlns.jcp.org/xml/ns/persistence/persistence_2_1.xsd", "2.1"),
    JPA_2_2("http://xmlns.jcp.org/xml/ns/persistence/persistence_2_2.xsd", "2.2"),
    JPA_3_0("https://jakarta.ee/xml/ns/persistence/persistence_3_0.xsd", "3.0"),
    JPA_3_2("https://jakarta.ee/xml/ns/persistence/persistence_3_2.xsd", "3.2");


    private static final Map<String, Version> locationBindings = new HashMap<String, Version>();
    private static final Map<String, Version> versionBindings = new HashMap<String, Version>();

    private final String location;
    private final String version;

    Version(String location, String version) {
        this.location = location;
        this.version = version;
    }

    public String getVersion() {
        return version;
    }

    static {
        for (Version version : values()) {
            locationBindings.put(version.location, version);
            versionBindings.put(version.version, version);
        }
    }

    public static Version forLocation(final String location) {
        final Version version = locationBindings.get(location);
        return version != null ? version : UNKNOWN;
    }

    public static Version forVersion(final String version) {
        final Version result = versionBindings.get(version);
        return result != null ? result : UNKNOWN;
    }
}
