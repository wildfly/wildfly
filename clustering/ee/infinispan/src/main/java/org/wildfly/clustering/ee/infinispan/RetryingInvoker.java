/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2014, Red Hat, Inc., and individual contributors
 * as indicated by the @author tags. See the copyright.txt file in the
 * distribution for a full listing of individual contributors.
 *
 * This is free software; you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation; either version 2.1 of
 * the License, or (at your option) any later version.
 *
 * This software is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this software; if not, write to the Free
 * Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA
 * 02110-1301 USA, or see the FSF site: http://www.fsf.org.
 */
package org.wildfly.clustering.ee.infinispan;

import java.util.concurrent.Callable;
import java.util.concurrent.TimeUnit;

import org.jboss.logging.Logger;
import org.wildfly.clustering.ee.Invoker;

/**
 * A invocation strategy that invokes a given task, retrying a configurable number of times on failure using backoff sleep intervals.
 * @author Paul Ferraro
 */
public class RetryingInvoker implements Invoker {

    private static final Logger LOGGER = Logger.getLogger(RetryingInvoker.class);

    private final long[] backOffIntervals;
    private final TimeUnit unit;

    public RetryingInvoker(long... backOffIntervals) {
        this(backOffIntervals, TimeUnit.MILLISECONDS);
    }

    public RetryingInvoker(long[] backOffIntervals, TimeUnit unit) {
        this.backOffIntervals = backOffIntervals;
        this.unit = unit;
    }

    @Override
    public <R> R invoke(Callable<R> task) throws Exception {
        Exception exception = null;

        for (int i = 0; i < this.backOffIntervals.length; ++i) {
            if (i > 0) {
                long delay = this.backOffIntervals[i];
                if (delay > 0) {
                    try {
                        this.unit.sleep(delay);
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                    }
                } else {
                    Thread.yield();
                }
                if (Thread.currentThread().isInterrupted()) break;
            }
            try {
                return task.call();
            } catch (Exception e) {
                exception = e;
            }
            LOGGER.debugf(exception, "Attempt #%d failed", i + 1);
        }

        throw exception;
    }
}
