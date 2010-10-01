/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2010, Red Hat, Inc., and individual contributors
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

package org.jboss.as.threads;

import org.jboss.msc.inject.Injector;
import org.jboss.msc.service.Service;
import org.jboss.msc.service.StartContext;
import org.jboss.msc.service.StartException;
import org.jboss.msc.service.StopContext;
import org.jboss.msc.value.InjectedValue;
import org.jboss.threads.JBossExecutors;

import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.ThreadFactory;

/**
 * Service responsible for creating, starting and stopping a scheduled thread pool executor.
 *
 * @author <a href="mailto:david.lloyd@redhat.com">David M. Lloyd</a>
 */
public final class ScheduledThreadPoolService implements Service<ScheduledExecutorService> {

    private final InjectedValue<ThreadFactory> threadFactoryValue = new InjectedValue<ThreadFactory>();

    private ScheduledThreadPoolExecutor executor;
    private ScheduledExecutorService value;
    private StopContext context;

    private final int maxThreads;
    private final TimeSpec keepAlive;

    public ScheduledThreadPoolService(final int maxThreads, final TimeSpec keepAlive) {
        this.maxThreads = maxThreads;
        this.keepAlive = keepAlive;
    }

    public synchronized void start(final StartContext context) throws StartException {
        executor = new ExecutorImpl(0, threadFactoryValue.getValue());
        executor.setCorePoolSize(maxThreads);
        if(keepAlive != null)
            executor.setKeepAliveTime(keepAlive.getDuration(), keepAlive.getUnit());
        value = JBossExecutors.protectedScheduledExecutorService(executor);
    }

    public synchronized void stop(final StopContext context) {
        final ScheduledThreadPoolExecutor executor = this.executor;
        if (executor == null) {
            throw new IllegalStateException();
        }
        this.context = context;
        context.asynchronous();
        executor.shutdown();
        this.executor = null;
        value = null;
    }

    public synchronized ScheduledExecutorService getValue() throws IllegalStateException {
        final ScheduledExecutorService value = this.value;
        if (value == null) {
            throw new IllegalStateException();
        }
        return value;
    }

    public Injector<ThreadFactory> getThreadFactoryInjector() {
        return threadFactoryValue;
    }

    private class ExecutorImpl extends ScheduledThreadPoolExecutor {

        ExecutorImpl(final int corePoolSize, final ThreadFactory threadFactory) {
            super(corePoolSize, threadFactory);
        }

        protected void terminated() {
            synchronized (ScheduledThreadPoolService.this) {
                super.terminated();
                context.complete();
                context = null;
            }
        }
    }
}
