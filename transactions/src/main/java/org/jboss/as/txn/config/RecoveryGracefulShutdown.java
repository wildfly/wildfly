/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jboss.as.txn.config;

public enum RecoveryGracefulShutdown {

    IGNORE("ignore"),
    WAIT("wait");

    private final String name;

    RecoveryGracefulShutdown(String name) {
        this.name = name;
    }

    @Override
    public String toString() {
        return this.name;
    }
}
