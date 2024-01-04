/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.wildfly.iiop.openjdk;

/**
 * <p>
 * Enumeration of the allowed IIOP subsystem configuration values.
 * </p>
 *
 * @author <a href="mailto:sguilhen@redhat.com">Stefan Guilhen</a>
 */
enum TransactionsAllowedValues {

    FULL("full"), NONE("none"), SPEC("spec");

    private String name;

    TransactionsAllowedValues(String name) {
        this.name = name;
    }
    @Override
    public String toString() {
        return this.name;
    }
}
