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

import javax.enterprise.concurrent.Trigger;
import java.util.concurrent.Callable;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

/**
 * A {@link ComponentManagedScheduledExecutorService} which wraps tasks so these get invoked with a specific context set, and delegates executions into a {@link TaskDecoratorScheduledExecutorService}.
 *
 * @author Eduardo Martins
 */
public class ManagedScheduledExecutorServiceImpl extends ManagedExecutorServiceImpl implements ComponentManagedScheduledExecutorService {

    private final TaskDecoratorScheduledExecutorService scheduledExecutorService;

    public ManagedScheduledExecutorServiceImpl(TaskDecoratorScheduledExecutorService scheduledExecutorService, ContextConfiguration contextConfiguration) {
        super(scheduledExecutorService, contextConfiguration);
        this.scheduledExecutorService = scheduledExecutorService;
    }

    // ScheduledExecutorService delegation

    @Override
    public ScheduledFuture<?> schedule(Runnable task, long delay, TimeUnit unit) {
        task = wrap(task, false);
        try {
            return scheduledExecutorService.schedule(task, delay, unit);
        } catch (RuntimeException | Error e) {
            if (task instanceof TaskWrapper) {
                ((TaskWrapper) task).taskSubmitFailed(e);
            }
            throw e;
        }
    }

    @Override
    public <V> ScheduledFuture<V> schedule(Callable<V> task, long delay, TimeUnit unit) {
        task = wrap(task, false);
        try {
            return scheduledExecutorService.schedule(task, delay, unit);
        } catch (RuntimeException | Error e) {
            if (task instanceof TaskWrapper) {
                ((TaskWrapper) task).taskSubmitFailed(e);
            }
            throw e;
        }
    }

    @Override
    public ScheduledFuture<?> scheduleAtFixedRate(Runnable task, long initialDelay, long period, TimeUnit unit) {
        task = wrap(task, true);
        try {
            return scheduledExecutorService.scheduleAtFixedRate(task, initialDelay, period, unit);
        } catch (RuntimeException | Error e) {
            if (task instanceof TaskWrapper) {
                ((TaskWrapper) task).taskSubmitFailed(e);
            }
            throw e;
        }
    }

    @Override
    public ScheduledFuture<?> scheduleWithFixedDelay(Runnable task, long initialDelay, long delay, TimeUnit unit) {
        task = wrap(task, true);
        try {
            return scheduledExecutorService.scheduleWithFixedDelay(task, initialDelay, delay, unit);
        } catch (RuntimeException | Error e) {
            if (task instanceof TaskWrapper) {
                ((TaskWrapper) task).taskSubmitFailed(e);
            }
            throw e;
        }
    }

    // ManagedScheduledExecutorService impl

    @Override
    public <V> ScheduledFuture<V> schedule(Callable<V> task, Trigger trigger) {
        checkShutdownState();
        return new TriggerScheduledFuture<>(task, trigger, this);
    }

    @Override
    public ScheduledFuture<?> schedule(Runnable task, Trigger trigger) {
        checkShutdownState();
        return new TriggerScheduledFuture<>(task, trigger, this);
    }

}
