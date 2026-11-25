/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.jboss.as.ee.concurrent;

import jakarta.enterprise.concurrent.ManagedExecutorService;

/**
 *
 * @author Eduardo Martins
 */
public interface WildFlyManagedExecutorService extends ManagedExecutorService {

    WildFlyManagedThreadFactory getWildFlyManagedThreadFactory();

    enum RejectPolicy {
        ABORT, RETRY_ABORT
    }

    /**
     * Attempts to terminate the executor's hung tasks, by cancelling such tasks.
     */
    void terminateHungTasks();

    ManagedExecutorRuntimeStats getRuntimeStats();
}
