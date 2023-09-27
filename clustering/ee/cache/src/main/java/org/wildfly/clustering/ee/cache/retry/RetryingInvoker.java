/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.wildfly.clustering.ee.cache.retry;

import java.time.Duration;
import java.util.Arrays;
import java.util.List;

import org.jboss.logging.Logger;
import org.wildfly.clustering.ee.Invoker;
import org.wildfly.common.function.ExceptionRunnable;
import org.wildfly.common.function.ExceptionSupplier;

/**
 * A invocation strategy that invokes a given task, retrying a configurable number of times on failure using backoff sleep intervals.
 * @author Paul Ferraro
 */
public class RetryingInvoker implements Invoker {

    // No logger interface for this module and no reason to create one for this class only
    private static final Logger LOGGER = Logger.getLogger(RetryingInvoker.class);

    private final List<Duration> retryIntevals;

    public RetryingInvoker(Duration... retryIntervals) {
        this(Arrays.asList(retryIntervals));
    }

    protected RetryingInvoker(List<Duration> retryIntevals) {
        this.retryIntevals = retryIntevals;
    }

    @Override
    public <R, E extends Exception> R invoke(ExceptionSupplier<R, E> task) throws E {
        int attempt = 0;
        for (Duration delay : this.retryIntevals) {
            if (Thread.currentThread().isInterrupted()) break;
            try {
                return task.get();
            } catch (Exception e) {
                LOGGER.debugf(e, "Attempt #%d failed", ++attempt);
            }
            if (delay.isZero() || delay.isNegative()) {
                Thread.yield();
            } else {
                try {
                    Thread.sleep(delay.toMillis(), delay.getNano() % 1_000_000);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
            }
        }
        return task.get();
    }

    @Override
    public <E extends Exception> void invoke(ExceptionRunnable<E> action) throws E {
        ExceptionSupplier<Void, E> adapter = new ExceptionSupplier<>() {
            @Override
            public Void get() throws E {
                action.run();
                return null;
            }
        };
        this.invoke(adapter);
    }
}
