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
package org.wildfly.as.concurrent.context;

import java.util.concurrent.Callable;
import java.util.concurrent.TimeUnit;

/**
 * A {@link Callable} {@link TriggerContextualTask}.
 *
 * @author Eduardo Martins
 */
public class TriggerContextualTaskCallable<V> extends TriggerContextualTask<V> implements Callable<V> {

    private final Context context;
    private final Callable<V> task;

    /**
     * @param task
     * @param executor
     * @param parent
     */
    protected TriggerContextualTaskCallable(Callable<V> task, ContextualManagedScheduledExecutorService executor, TriggerScheduledFuture<V> parent) {
        super(task, executor, parent);
        this.task = task;
        context = executor.getContextConfiguration() != null ? executor.getContextConfiguration().newTaskContext(task) : null;
    }

    /**
     * @return
     */
    protected Context getContext() {
        return context;
    }

    @Override
    public V call() throws Exception {
        if (!getCurrentExecution().isSkipped()) {
            final Context previousContext = Utils.setContext(context);
            try {
                final V v = task.call();
                getCurrentExecution().setResult(v);
                return v;
            } finally {
                Utils.setContext(previousContext);
            }
        } else {
            return null;
        }
    }

    @Override
    protected void scheduleTask(long delay, TimeUnit timeUnit) {
        scheduler.schedule(this, delay, timeUnit);
    }
}
