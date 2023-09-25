/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jboss.as.test.clustering.cluster.ejb2.stateful.failover.bean.singleton;

import jakarta.ejb.Remote;

/**
 * @author Ondrej Chaloupka
 */
@Remote
public interface CounterSingletonRemote {
    int getDestroyCount();

    void resetDestroyCount();
}
