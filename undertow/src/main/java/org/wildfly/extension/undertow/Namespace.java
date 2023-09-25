/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.wildfly.extension.undertow;

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
    UNDERTOW_7_0("urn:jboss:domain:undertow:7.0"),
    UNDERTOW_8_0("urn:jboss:domain:undertow:8.0"),
    UNDERTOW_9_0("urn:jboss:domain:undertow:9.0"),
    UNDERTOW_10_0("urn:jboss:domain:undertow:10.0"),
    UNDERTOW_11_0("urn:jboss:domain:undertow:11.0"),
    UNDERTOW_12_0("urn:jboss:domain:undertow:12.0"),
    UNDERTOW_13_0("urn:jboss:domain:undertow:13.0"),
    ;

    /**
     * The current namespace version.
     */
    public static final Namespace CURRENT = UNDERTOW_13_0;

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
}
