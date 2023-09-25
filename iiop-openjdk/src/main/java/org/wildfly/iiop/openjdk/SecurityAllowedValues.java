/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.wildfly.iiop.openjdk;

/**
 * <p>
 * Enumeration of the allowed iiop subsystem configuration values.
 * </p>
 *
 * @author <a href="mailto:sguilhen@redhat.com">Stefan Guilhen</a>
 */
enum SecurityAllowedValues {

    IDENTITY("identity"),
    CLIENT("client"),
    ELYTRON("elytron"),
    NONE("none");

    private String name;

    SecurityAllowedValues(String name) {
        this.name = name;
    }
    @Override
    public String toString() {
        return this.name;
    }
}
