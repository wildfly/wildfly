/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.jboss.as.test.clustering.cluster.ejb.remote.bean;

import jakarta.ejb.Remove;

public interface Incrementor {
    Result<Integer> increment();

    @Remove
    default void remove() {
        // Do nothing
    }
}
