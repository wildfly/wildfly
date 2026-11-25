/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.jboss.as.ee.concurrent.adapter;

import jakarta.enterprise.concurrent.ManagedExecutorService;
import org.jboss.as.ee.logging.EeLogger;

import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * Abstract base class for {@code ManagedExecutorService} and {@code ManagedScheduledExecutorService} implementation with life cycle operations disabled for handing out to application components.
 */
public abstract class AbstractManagedExecutorServiceAdapter implements ManagedExecutorService {

    @Override
    public void shutdown() {
        throw EeLogger.ROOT_LOGGER.lifecycleOperationNotSupported();
    }

    @Override
    public List<Runnable> shutdownNow() {
        throw EeLogger.ROOT_LOGGER.lifecycleOperationNotSupported();
    }

    @Override
    public boolean isShutdown() {
        throw EeLogger.ROOT_LOGGER.lifecycleOperationNotSupported();
    }

    @Override
    public boolean isTerminated() {
        throw EeLogger.ROOT_LOGGER.lifecycleOperationNotSupported();
    }

    @Override
    public boolean awaitTermination(long timeout, TimeUnit unit) throws InterruptedException {
        throw EeLogger.ROOT_LOGGER.lifecycleOperationNotSupported();
    }
}
