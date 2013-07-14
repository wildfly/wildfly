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
import org.wildfly.ee.concurrent.TaskDecoratorScheduledExecutorService;
import org.wildfly.ee.concurrent.TaskDecoratorScheduledThreadPoolExecutor;

import java.util.concurrent.ThreadFactory;

/**
 * Service responsible for creating, starting and stopping a {@link TaskDecoratorScheduledExecutorService}, to be shared by multiple {@link org.wildfly.ee.concurrent.ManagedScheduledExecutorServiceImpl}.
 *
 * @author Eduardo Martins
 */
public final class TaskDecoratorScheduledExecutorServiceService extends AbstractTaskDecoratorExecutorServiceService<TaskDecoratorScheduledExecutorService> {

    public TaskDecoratorScheduledExecutorServiceService() {
        this(10, null);
    }

    public TaskDecoratorScheduledExecutorServiceService(final int maxThreads, final TimeSpec keepAlive) {
        super(maxThreads, keepAlive);
    }

    @Override
    protected TaskDecoratorScheduledExecutorService newExecutor() {
        final ExecutorImpl executor = new ExecutorImpl(maxThreads, threadFactoryValue.getValue());
        if (keepAlive != null) {
            executor.setKeepAliveTime(keepAlive.getDuration(), keepAlive.getUnit());
        }
        return executor;
    }

    private class ExecutorImpl extends TaskDecoratorScheduledThreadPoolExecutor {

        ExecutorImpl(final int corePoolSize, final ThreadFactory threadFactory) {
            super(corePoolSize, threadFactory);
        }

        protected void terminated() {
            super.terminated();
            TaskDecoratorScheduledExecutorServiceService.this.executorTerminated();
        }
    }
}
