/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.jboss.as.ee.subsystem;

import java.util.HashMap;
import java.util.Map;

/**
 * @author Stuart Douglas
 */
enum Namespace {
    // must be first
    UNKNOWN(null, false),

    EE_1_0("urn:jboss:domain:ee:1.0", true),
    EE_1_1("urn:jboss:domain:ee:1.1", true),
    EE_1_2("urn:jboss:domain:ee:1.2", true),
    EE_2_0("urn:jboss:domain:ee:2.0", true),
    EE_3_0("urn:jboss:domain:ee:3.0", false),
    EE_4_0("urn:jboss:domain:ee:4.0", false),
    EE_5_0("urn:jboss:domain:ee:5.0", false),
    EE_6_0("urn:jboss:domain:ee:6.0", false)
    ;
    /**
     * The current namespace version.
     */
    public static final Namespace CURRENT = EE_6_0;

    private final String name;
    private final boolean beanValidationIncluded;

    Namespace(final String name, final boolean beanValidationIncluded) {
        this.name = name;
        this.beanValidationIncluded = beanValidationIncluded;
    }

    /**
     * Get the URI of this namespace.
     *
     * @return the URI
     */
    public String getUriString() {
        return name;
    }

    public boolean isBeanValidationIncluded() {
        return beanValidationIncluded;
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
