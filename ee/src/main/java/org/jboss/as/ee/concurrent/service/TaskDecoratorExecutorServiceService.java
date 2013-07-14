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

package org.jboss.as.ee.concurrent.service;

import org.jboss.as.ee.concurrent.TimeSpec;
import org.wildfly.ee.concurrent.TaskDecoratorExecutorService;
import org.wildfly.ee.concurrent.TaskDecoratorThreadPoolExecutor;

import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;

/**
 * Service responsible for creating, starting and stopping a {@link TaskDecoratorExecutorService}, to be shared by multiple {@link org.wildfly.ee.concurrent.ManagedExecutorServiceImpl}.
 *
 * @author Eduardo Martins
 */
public final class TaskDecoratorExecutorServiceService extends AbstractTaskDecoratorExecutorServiceService<TaskDecoratorExecutorService> {

    public TaskDecoratorExecutorServiceService() {
        this(25, null);
    }

    public TaskDecoratorExecutorServiceService(final int maxThreads, final TimeSpec keepAlive) {
        super(maxThreads, keepAlive);
    }

    @Override
    protected TaskDecoratorExecutorService newExecutor() {
        long keepAliveTime = 0L;
        TimeUnit unit = TimeUnit.MILLISECONDS;
        if (keepAlive != null) {
            keepAliveTime = keepAlive.getDuration();
            unit = keepAlive.getUnit();
        }
        return new ExecutorImpl(maxThreads, keepAliveTime, unit, threadFactoryValue.getValue());
    }

    private class ExecutorImpl extends TaskDecoratorThreadPoolExecutor {

        ExecutorImpl(final int maxThreads, final long keepAliveTime,
                     final TimeUnit unit, final ThreadFactory threadFactory) {
            super(maxThreads, maxThreads, keepAliveTime, unit, new LinkedBlockingQueue<Runnable>(), threadFactory);
        }

        protected void terminated() {
            super.terminated();
            TaskDecoratorExecutorServiceService.this.executorTerminated();
        }
    }
}
