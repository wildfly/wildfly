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
import org.jboss.threads.EventListener;
import org.jboss.threads.JBossExecutors;
import org.jboss.threads.JBossThreadPoolExecutor;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;

/**
 * Service responsible for creating, starting and stopping a thread pool executor with an unbounded queue.
 *
 * @author John E. Bailey
 */
public class UnboundedQueueThreadPoolService implements Service<ExecutorService> {
    private final InjectedValue<ThreadFactory> threadFactoryValue = new InjectedValue<ThreadFactory>();

    private JBossThreadPoolExecutor executor;
    private ExecutorService value;

    private int maxThreads;
    private TimeSpec keepAlive;

    public UnboundedQueueThreadPoolService(int maxThreads, TimeSpec keepAlive) {
        this.maxThreads = maxThreads;
        this.keepAlive = keepAlive;
    }

    public synchronized void start(final StartContext context) throws StartException {
        final TimeSpec keepAliveSpec = keepAlive;
        long keepAliveTime = keepAliveSpec == null ? Long.MAX_VALUE : keepAliveSpec.getUnit().toNanos(keepAliveSpec.getDuration());
        executor = new JBossThreadPoolExecutor(maxThreads, maxThreads, keepAliveTime, TimeUnit.NANOSECONDS, new LinkedBlockingQueue<Runnable>(), threadFactoryValue.getValue());
        value = JBossExecutors.protectedExecutorService(executor);
    }

    public synchronized void stop(final StopContext context) {
        final JBossThreadPoolExecutor executor = this.executor;
        if (executor == null) {
            throw new IllegalStateException();
        }
        context.asynchronous();
        executor.shutdown();
        executor.addShutdownListener(new EventListener<StopContext>() {
            public void handleEvent(final StopContext stopContext) {
                stopContext.complete();
            }
        }, context);
        this.executor = null;
        value = null;
    }

    public synchronized ExecutorService getValue() throws IllegalStateException {
        final ExecutorService value = this.value;
        if (value == null) {
            throw new IllegalStateException();
        }
        return value;
    }

    public Injector<ThreadFactory> getThreadFactoryInjector() {
        return threadFactoryValue;
    }

    public synchronized void setMaxThreads(final int maxThreads) {
        this.maxThreads = maxThreads;
        final JBossThreadPoolExecutor executor = this.executor;
        if(executor != null) {
            executor.setMaxThreads(maxThreads);
        }
    }

    public synchronized void setKeepAlive(final TimeSpec keepAlive) {
        this.keepAlive = keepAlive;
        final JBossThreadPoolExecutor executor = this.executor;
        if(executor != null) {
            executor.setKeepAliveTime(keepAlive.getDuration(), keepAlive.getUnit());
        }
    }
}
