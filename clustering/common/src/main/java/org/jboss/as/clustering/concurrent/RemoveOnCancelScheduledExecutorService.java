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
package org.jboss.as.clustering.concurrent;

import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.ThreadFactory;

import org.jboss.as.clustering.msc.AsynchronousService;
import org.jboss.msc.service.Service;
import org.jboss.msc.service.ServiceBuilder;
import org.jboss.msc.service.ServiceName;
import org.jboss.msc.service.ServiceTarget;
import org.jboss.msc.service.StartContext;
import org.jboss.msc.service.StopContext;
import org.jboss.threads.JBossExecutors;

/**
 * Service that provides a {@link ScheduledThreadPoolExecutor} that removes tasks from the task queue upon cancellation.
 * @author Paul Ferraro
 */
public class RemoveOnCancelScheduledExecutorService implements Service<ScheduledExecutorService> {

    public static ServiceBuilder<ScheduledExecutorService> build(ServiceTarget target, ServiceName name, ThreadFactory factory) {
        return build(target, name, factory, 1);
    }

    public static ServiceBuilder<ScheduledExecutorService> build(ServiceTarget target, ServiceName name, ThreadFactory factory, int size) {
        return AsynchronousService.addService(target, name, new RemoveOnCancelScheduledExecutorService(size, factory), false, true);
    }

    private final ThreadFactory threadFactory;
    private final int size;

    private volatile ScheduledExecutorService executor;

    private RemoveOnCancelScheduledExecutorService(int size, ThreadFactory threadFactory) {
        this.size = size;
        this.threadFactory = threadFactory;
    }

    @Override
    public ScheduledExecutorService getValue() {
        return JBossExecutors.protectedScheduledExecutorService(this.executor);
    }

    @Override
    public void start(StartContext context) {
        ScheduledThreadPoolExecutor executor = new ScheduledThreadPoolExecutor(this.size, this.threadFactory);
        executor.setRemoveOnCancelPolicy(true);
        executor.setExecuteExistingDelayedTasksAfterShutdownPolicy(false);
        this.executor = executor;
    }

    @Override
    public void stop(StopContext context) {
        this.executor.shutdown();
        this.executor = null;
    }
}
