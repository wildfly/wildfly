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
import org.jboss.threads.QueuelessExecutor;

import java.util.concurrent.Executor;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;

/**
 * Service responsible for creating, starting and stopping a thread pool executor with no queue.
 *
 * @author John E. Bailey
 */
public class QueuelessThreadPoolService implements Service<ManagedQueuelessExecutorService> {
    private final InjectedValue<ThreadFactory> threadFactoryValue = new InjectedValue<ThreadFactory>();
    private final InjectedValue<Executor> handoffExecutorValue = new InjectedValue<Executor>();

    private ManagedQueuelessExecutorService executor;

    private int maxThreads;
    private boolean blocking;
    private TimeSpec keepAlive;

    public QueuelessThreadPoolService(int maxThreads, boolean blocking, TimeSpec keepAlive) {
        this.maxThreads = maxThreads;
        this.blocking = blocking;
        this.keepAlive = keepAlive;
    }

    public synchronized void start(final StartContext context) throws StartException {
        final TimeSpec keepAliveSpec = keepAlive;
        long keepAlive = keepAliveSpec == null ? Long.MAX_VALUE : keepAliveSpec.getUnit().toMillis(keepAliveSpec.getDuration());
        final QueuelessExecutor queuelessExecutor = new QueuelessExecutor(threadFactoryValue.getValue(), JBossExecutors.directExecutor(), handoffExecutorValue.getOptionalValue(), keepAlive);
        queuelessExecutor.setMaxThreads(maxThreads);
        queuelessExecutor.setBlocking(blocking);
        executor = new ManagedQueuelessExecutorService(queuelessExecutor);
    }

    public synchronized void stop(final StopContext context) {
        final ManagedQueuelessExecutorService executor = getValue();
        context.asynchronous();
        executor.internalShutdown();
        executor.addShutdownListener(new EventListener<StopContext>() {
            public void handleEvent(final StopContext stopContext) {
                stopContext.complete();
            }
        }, context);
        this.executor = null;
    }

    public synchronized ManagedQueuelessExecutorService getValue() throws IllegalStateException {
        final ManagedQueuelessExecutorService value = this.executor;
        if (value == null) {
            throw ThreadsMessages.MESSAGES.queuelessThreadPoolExecutorUninitialized();
        }
        return value;
    }

    public Injector<ThreadFactory> getThreadFactoryInjector() {
        return threadFactoryValue;
    }

    public Injector<Executor> getHandoffExecutorInjector() {
        return handoffExecutorValue;
    }

    public synchronized void setMaxThreads(int maxThreads) {
        this.maxThreads = maxThreads;
        final ManagedQueuelessExecutorService executor = this.executor;
        if(executor != null) {
            executor.setMaxThreads(maxThreads);
        }
    }

    public synchronized void setKeepAlive(TimeSpec keepAliveSpec) {
        keepAlive = keepAliveSpec;
        final ManagedQueuelessExecutorService executor = this.executor;
        if(executor != null) {
            long keepAlive = keepAliveSpec == null ? Long.MAX_VALUE : keepAliveSpec.getDuration();
            executor.setKeepAlive(keepAlive);
        }
    }

    public int getCurrentThreadCount() {
        final ManagedQueuelessExecutorService executor = getValue();
        return executor.getCurrentThreadCount();
    }

    public int getLargestThreadCount() {
        final ManagedQueuelessExecutorService executor = getValue();
        return executor.getLargestThreadCount();
    }

    public int getRejectedCount() {
        final ManagedQueuelessExecutorService executor = getValue();
        return executor.getRejectedCount();
    }

    TimeUnit getKeepAliveUnit() {
        return keepAlive == null ? TimeSpec.DEFAULT_KEEPALIVE.getUnit() : keepAlive.getUnit();
    }
}
