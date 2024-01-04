/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.wildfly.clustering.service.concurrent;

import java.util.Optional;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.locks.StampedLock;
import java.util.function.Supplier;

import org.wildfly.common.function.ExceptionRunnable;
import org.wildfly.common.function.ExceptionSupplier;

/**
 * {@link ServiceExecutor} implemented via a {@link StampedLock}.
 * @author Paul Ferraro
 * @deprecated To be removed without replacement
 */
@Deprecated(forRemoval = true)
public class StampedLockServiceExecutor implements ServiceExecutor {

    private final StampedLock lock = new StampedLock();
    private final AtomicBoolean closed = new AtomicBoolean(false);

    @Override
    public void execute(Runnable executeTask) {
        long stamp = this.lock.tryReadLock();
        if (stamp != 0L) {
            try {
                executeTask.run();
            } finally {
                this.lock.unlock(stamp);
            }
        }
    }

    @Override
    public <E extends Exception> void execute(ExceptionRunnable<E> executeTask) throws E {
        long stamp = this.lock.tryReadLock();
        if (stamp != 0L) {
            try {
                executeTask.run();
            } finally {
                this.lock.unlock(stamp);
            }
        }
    }

    @Override
    public <R> Optional<R> execute(Supplier<R> executeTask) {
        long stamp = this.lock.tryReadLock();
        if (stamp != 0L) {
            try {
                return Optional.of(executeTask.get());
            } finally {
                this.lock.unlock(stamp);
            }
        }
        return Optional.empty();
    }

    @Override
    public <R, E extends Exception> Optional<R> execute(ExceptionSupplier<R, E> executeTask) throws E {
        long stamp = this.lock.tryReadLock();
        if (stamp != 0L) {
            try {
                return Optional.of(executeTask.get());
            } finally {
                this.lock.unlock(stamp);
            }
        }
        return Optional.empty();
    }

    @Override
    public void close(Runnable closeTask) {
        // Allow only one thread to close
        if (this.closed.compareAndSet(false, true)) {
            // Closing is final - we don't need the stamp
            this.lock.writeLock();
            closeTask.run();
        }
    }
}
