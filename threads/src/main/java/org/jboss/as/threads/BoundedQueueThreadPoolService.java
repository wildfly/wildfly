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
import org.jboss.threads.QueueExecutor;

import java.util.concurrent.Executor;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;

/**
 * Service responsible for creating, starting and stopping a thread pool executor with a bounded queue.
 *
 * @author John E. Bailey
 */
public class BoundedQueueThreadPoolService implements Service<ManagedQueueExecutorService> {
    private final InjectedValue<ThreadFactory> threadFactoryValue = new InjectedValue<ThreadFactory>();
    private final InjectedValue<Executor> handoffExecutorValue = new InjectedValue<Executor>();

    private ManagedQueueExecutorService executor;

    private int coreThreads;
    private int maxThreads;
    private int queueLength;
    private boolean blocking;
    private TimeSpec keepAlive;
    private boolean allowCoreTimeout;

    public BoundedQueueThreadPoolService(int coreThreads, int maxThreads, int queueLength, boolean blocking, TimeSpec keepAlive, boolean allowCoreTimeout) {
        this.coreThreads = coreThreads;
        this.maxThreads = maxThreads;
        this.queueLength = queueLength;
        this.blocking = blocking;
        this.keepAlive = keepAlive;
        this.allowCoreTimeout = allowCoreTimeout;
    }

    public synchronized void start(final StartContext context) throws StartException {
        final TimeSpec keepAliveSpec = keepAlive;
        long keepAliveTime = keepAliveSpec == null ? Long.MAX_VALUE : keepAliveSpec.getUnit().toNanos(keepAliveSpec.getDuration());
        QueueExecutor queueExecutor = new QueueExecutor(coreThreads, maxThreads, keepAliveTime, TimeUnit.NANOSECONDS, queueLength, threadFactoryValue.getValue(), blocking, handoffExecutorValue.getOptionalValue());
        queueExecutor.setAllowCoreThreadTimeout(allowCoreTimeout);
        executor = new ManagedQueueExecutorService(queueExecutor);
    }

    public synchronized void stop(final StopContext context) {
        final ManagedQueueExecutorService executor = getValue();
        context.asynchronous();
        executor.internalShutdown();
        executor.addShutdownListener(new EventListener<StopContext>() {
            public void handleEvent(final StopContext stopContext) {
                stopContext.complete();
            }
        }, context);
        this.executor = null;
    }

    public synchronized ManagedQueueExecutorService getValue() throws IllegalStateException {
        final ManagedQueueExecutorService value = this.executor;
        if (value == null) {
            throw ThreadsMessages.MESSAGES.boundedQueueThreadPoolExecutorUninitialized();
        }
        return value;
    }

    public Injector<ThreadFactory> getThreadFactoryInjector() {
        return threadFactoryValue;
    }

    public Injector<Executor> getHandoffExecutorInjector() {
        return handoffExecutorValue;
    }

    public synchronized void setCoreThreads(int coreThreads) {
        this.coreThreads = coreThreads;
        final ManagedQueueExecutorService executor = this.executor;
        if(executor != null) {
            executor.setCoreThreads(coreThreads);
        }
    }

    public synchronized void setMaxThreads(int maxThreads) {
        this.maxThreads = maxThreads;
        final ManagedQueueExecutorService executor = this.executor;
        if(executor != null) {
            executor.setMaxThreads(maxThreads);
        }
    }

    public synchronized void setKeepAlive(TimeSpec keepAlive) {
        this.keepAlive = keepAlive;
        final ManagedQueueExecutorService executor = this.executor;
        if(executor != null) {
            executor.setKeepAlive(keepAlive);
        }
    }

    public synchronized void setAllowCoreTimeout(boolean allowCoreTimeout) {
        this.allowCoreTimeout = allowCoreTimeout;
        final ManagedQueueExecutorService executor = this.executor;
        if(executor != null) {
            executor.setAllowCoreTimeout(allowCoreTimeout);
        }
    }

    public int getCurrentThreadCount() {
        final ManagedQueueExecutorService executor = getValue();
        return executor.getCurrentThreadCount();
    }

    public int getLargestThreadCount() {
        final ManagedQueueExecutorService executor = getValue();
        return executor.getLargestThreadCount();
    }

    TimeUnit getKeepAliveUnit() {
        return keepAlive == null ? TimeSpec.DEFAULT_KEEPALIVE.getUnit() : keepAlive.getUnit();
    }

    public int getRejectedCount() {
        final ManagedQueueExecutorService executor = getValue();
        return executor.getRejectedCount();
    }
}
