/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.jboss.as.test.clustering.cluster.modcluster;

/**
 * Enumerates mod_cluster context status values as reported by the undertow mod-cluster filter.
 *
 * @author Radoslav Husar
 */
enum ModClusterContextStatus {
    ENABLED("enabled"),
    DISABLED("disabled"),
    STOPPED("stopped"),
    ;

    private final String value;

    ModClusterContextStatus(String value) {
        this.value = value;
    }

    @Override
    public String toString() {
        return this.value;
    }
}
