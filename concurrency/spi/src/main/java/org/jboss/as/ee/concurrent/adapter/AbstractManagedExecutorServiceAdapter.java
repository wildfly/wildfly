/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.jboss.as.ee.concurrent.adapter;

import jakarta.enterprise.concurrent.ManagedExecutorService;

import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * Abstract base class for {@code ManagedExecutorService} and {@code ManagedScheduledExecutorService} implementation with life cycle operations disabled for handing out to application components.
 */
public abstract class AbstractManagedExecutorServiceAdapter implements ManagedExecutorService {

    protected static final String LIFECYCLE_OPER_NOT_SUPPORTED = "Lifecycle operation not supported";

    @Override
    public void shutdown() {
        throw new IllegalStateException(LIFECYCLE_OPER_NOT_SUPPORTED);
    }

    @Override
    public List<Runnable> shutdownNow() {
        throw new IllegalStateException(LIFECYCLE_OPER_NOT_SUPPORTED);
    }

    @Override
    public boolean isShutdown() {
        throw new IllegalStateException(LIFECYCLE_OPER_NOT_SUPPORTED);
    }

    @Override
    public boolean isTerminated() {
        throw new IllegalStateException(LIFECYCLE_OPER_NOT_SUPPORTED);
    }

    @Override
    public boolean awaitTermination(long timeout, TimeUnit unit) throws InterruptedException {
        throw new IllegalStateException(LIFECYCLE_OPER_NOT_SUPPORTED);
    }
}
