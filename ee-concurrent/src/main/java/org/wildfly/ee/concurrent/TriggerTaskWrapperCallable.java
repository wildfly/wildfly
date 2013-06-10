/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2013, Red Hat, Inc., and individual contributors
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
package org.wildfly.ee.concurrent;

import java.util.concurrent.Callable;
import java.util.concurrent.TimeUnit;

/**
 * A {@link Callable} {@link TriggerTaskWrapper}.
 *
 * @author Eduardo Martins
 */
public class TriggerTaskWrapperCallable<V> extends TriggerTaskWrapper<V> implements Callable<V> {

    private final Callable<V> task;

    /**
     * @param task
     * @param executor
     * @param parent
     */
    protected TriggerTaskWrapperCallable(Callable<V> task, ManagedScheduledExecutorServiceImpl executor, TriggerScheduledFuture<V> parent) {
        super(task, executor, parent);
        final ContextConfiguration contextConfiguration = executor.getContextConfiguration();
        this.task = contextConfiguration != null ? contextConfiguration.newContextualCallable(task) : task;
    }

    @Override
    public V call() throws Exception {
        Throwable t = null;
        try {
            beforeExecution();
            if (!getCurrentExecution().isSkipped()) {
                final V v = task.call();
                getCurrentExecution().setResult(v);
                return v;
            } else {
                return null;
            }
        } catch (Error | Exception e) {
            t = e;
            throw e;
        } finally {
            afterExecution(t);
        }
    }

    @Override
    protected void scheduleTask(long delay, TimeUnit timeUnit) {
        scheduler.schedule(this, delay, timeUnit);
    }
}
