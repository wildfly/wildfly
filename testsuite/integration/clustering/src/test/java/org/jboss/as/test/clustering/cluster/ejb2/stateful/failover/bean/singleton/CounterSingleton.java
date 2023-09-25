/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jboss.as.test.clustering.cluster.ejb2.stateful.failover.bean.singleton;

import java.util.concurrent.atomic.AtomicInteger;

import jakarta.ejb.Singleton;
import jakarta.ejb.Startup;

import org.jboss.logging.Logger;

@Singleton
@Startup
public class CounterSingleton implements CounterSingletonRemote {
    public static AtomicInteger destroyCounter = new AtomicInteger(0);
    private static final Logger log = Logger.getLogger(CounterSingleton.class);

    public int getDestroyCount() {
        log.trace("destroyCounter: " + destroyCounter.get());
        return destroyCounter.get();
    }

    public void resetDestroyCount() {
        destroyCounter.set(0);
    }
}
