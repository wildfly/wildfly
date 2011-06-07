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
import java.util.concurrent.ExecutorService;
import java.util.concurrent.ThreadFactory;

/**
 * Service responsible for creating, starting and stopping a thread pool executor with no queue.
 *
 * @author John E. Bailey
 */
public class QueuelessThreadPoolService implements Service<ExecutorService> {
    private final InjectedValue<ThreadFactory> threadFactoryValue = new InjectedValue<ThreadFactory>();
    private final InjectedValue<Executor> handoffExecutorValue = new InjectedValue<Executor>();

    private QueuelessExecutor executor;
    private ExecutorService value;

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
        executor = new QueuelessExecutor(threadFactoryValue.getValue(), JBossExecutors.directExecutor(), handoffExecutorValue.getOptionalValue(), keepAlive);
        executor.setMaxThreads(maxThreads);
        executor.setBlocking(blocking);
        value = JBossExecutors.protectedExecutorService(executor);
    }

    public synchronized void stop(final StopContext context) {
        final QueuelessExecutor executor = this.executor;
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

    public Injector<Executor> getHandoffExecutorInjector() {
        return handoffExecutorValue;
    }

    public synchronized void setMaxThreads(int maxThreads) {
        this.maxThreads = maxThreads;
        final QueuelessExecutor executor = this.executor;
        if(executor != null) {
            executor.setMaxThreads(maxThreads);
        }
    }

    public synchronized void setBlocking(boolean blocking) {
        this.blocking = blocking;
        final QueuelessExecutor executor = this.executor;
        if(executor != null) {
            executor.setBlocking(blocking);
        }
    }

    public synchronized void setKeepAlive(TimeSpec keepAliveSpec) {
        keepAlive = keepAliveSpec;
        final QueuelessExecutor executor = this.executor;
        if(executor != null) {
            long keepAlive = keepAliveSpec == null ? Long.MAX_VALUE : keepAliveSpec.getDuration();
            executor.setKeepAliveTime(keepAlive);
        }
    }
}
