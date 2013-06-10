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

package org.wildfly.as.concurrent;

import org.wildfly.as.concurrent.component.ComponentManagedExecutorServiceShutdownHandler;
import org.wildfly.as.concurrent.component.ComponentManagedScheduledExecutorService;
import org.wildfly.as.concurrent.context.ContextConfiguration;
import org.wildfly.as.concurrent.context.ContextualManagedScheduledExecutorService;
import org.wildfly.as.concurrent.tasklistener.TaskListenerScheduledExecutorService;

import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.TimeUnit;

/**
 * A ManagedScheduledExecutorService for a specific EE component, which on internalShutdown cancels/interrupts all submitted/scheduled tasks.
 *
 * @author Eduardo Martins
 */
public class ManagedScheduledExecutorServiceImpl extends ContextualManagedScheduledExecutorService implements ComponentManagedScheduledExecutorService {

    /**
     * component ManagedExecutorService are required to cancel all tasks on shutdown, this handler is responsible for managing that.
     */
    private final ComponentManagedExecutorServiceShutdownHandler shutdownHandler;

    /**
     * @param executor
     * @param contextConfiguration
     */
    public ManagedScheduledExecutorServiceImpl(TaskListenerScheduledExecutorService executor, ContextConfiguration contextConfiguration) {
        super(executor, contextConfiguration);
        this.shutdownHandler = new ComponentManagedExecutorServiceShutdownHandler();
    }

    @Override
    public void internalShutdown() {
        // all lifecycle related methods are delegated to the shutdown handler
        shutdownHandler.internalShutdown();
    }

    @Override
    public void shutdown() {
        // all lifecycle related methods are delegated to the shutdown handler
        shutdownHandler.shutdown();
    }

    @Override
    public List<Runnable> shutdownNow() {
        // all lifecycle related methods are delegated to the shutdown handler
        return shutdownHandler.shutdownNow();
    }

    @Override
    public boolean isShutdown() {
        // all lifecycle related methods are delegated to the shutdown handler
        return shutdownHandler.isShutdown();
    }

    @Override
    public boolean isTerminated() {
        // all lifecycle related methods are delegated to the shutdown handler
        return shutdownHandler.isTerminated();
    }

    @Override
    public boolean awaitTermination(long timeout, TimeUnit unit) throws InterruptedException {
        // all lifecycle related methods are delegated to the shutdown handler
        return shutdownHandler.awaitTermination(timeout, unit);
    }

    @Override
    protected Runnable wrap(Runnable task) {
        // use the shutdown handler to wrap the task, so it becomes managed
        return shutdownHandler.wrap(super.wrap(task));
    }

    @Override
    protected <V> Callable<V> wrap(Callable<V> task) {
        // use the shutdown handler to wrap the task, so it becomes managed
        return shutdownHandler.wrap(super.wrap(task));
    }

}
