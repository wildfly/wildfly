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

package org.jboss.as.remoting;

import org.jboss.msc.service.Service;
import org.jboss.msc.service.ServiceName;
import org.jboss.msc.service.StartContext;
import org.jboss.msc.service.StartException;
import org.jboss.msc.service.StopContext;
import org.jboss.threads.JBossExecutors;

import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.RejectedExecutionHandler;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

/**
 * A thread pool executor service, which is configurable at runtime.
 *
 * @author <a href="mailto:david.lloyd@redhat.com">David M. Lloyd</a>
 */
public final class ThreadPoolExecutorService implements Service<ExecutorService> {

    private static final int DEFAULT_QUEUE_LENGTH = 100;

    private boolean allowCoreTimeout = false;
    private int corePoolSize = 10;
    private int maximumPoolSize = 40;
    private long keepAliveTime = 30L;
    private TimeUnit unit = TimeUnit.SECONDS;
    private BlockingQueue<Runnable> workQueue = new ArrayBlockingQueue<Runnable>(DEFAULT_QUEUE_LENGTH);
    private ThreadFactory threadFactory = Executors.defaultThreadFactory();
    private RejectedExecutionHandler handler = new ThreadPoolExecutor.AbortPolicy();

    /**
     * The service name under which thread-related services are registered.
     */
    public static ServiceName JBOSS_THREADS = ServiceName.JBOSS.append("threads");
    /**
     * The service name under which executors (thread pools) are registered.
     */
    public static ServiceName JBOSS_THREADS_EXECUTOR = JBOSS_THREADS.append("executor");

    /**
     * Construct a new instance.
     */
    public ThreadPoolExecutorService() {
    }

    /**
     * Determine whether core threads are allowed to time out.
     *
     * @return {@code true} if core threads are allowed to time out
     */
    public synchronized boolean isAllowCoreTimeout() {
        return allowCoreTimeout;
    }

    /**
     * Specify whether core threads are allowed to time out.  If the service is already started, the change will
     * take effect immediately.
     *
     * @param allowCoreTimeout {@code true} if core threads are allowed to time out
     */
    public synchronized void setAllowCoreTimeout(final boolean allowCoreTimeout) {
        this.allowCoreTimeout = allowCoreTimeout;
        final ThreadPoolExecutor realExecutor = this.realExecutor;
        if (realExecutor != null) {
            realExecutor.allowCoreThreadTimeOut(allowCoreTimeout);
        }
    }

    /**
     * Get the configured core pool size.
     *
     * @return the core pool size
     */
    public synchronized int getCorePoolSize() {
        return corePoolSize;
    }

    /**
     * Set the configured core pool size.  If the service is already started, the change will take effect immediately.
     *
     * @param corePoolSize the core pool size
     */
    public synchronized void setCorePoolSize(final int corePoolSize) {
        this.corePoolSize = corePoolSize;
        final ThreadPoolExecutor realExecutor = this.realExecutor;
        if (realExecutor != null) {
            realExecutor.setCorePoolSize(corePoolSize);
        }
    }

    /**
     * Get the configured maximum pool size.
     *
     * @return the maximum pool size
     */
    public synchronized int getMaximumPoolSize() {
        return maximumPoolSize;
    }

    /**
     * Set the configured maximum pool size.  If the service is already started, the change will take effect immediately.
     *
     * @param maximumPoolSize the maximum pool size
     */
    public synchronized void setMaximumPoolSize(final int maximumPoolSize) {
        this.maximumPoolSize = maximumPoolSize;
        final ThreadPoolExecutor realExecutor = this.realExecutor;
        if (realExecutor != null) {
            realExecutor.setMaximumPoolSize(maximumPoolSize);
        }
    }

    /**
     * Get the keep-alive time.
     *
     * @param unit the units to use
     * @return the keep-alive time
     */
    public synchronized long getKeepAliveTime(final TimeUnit unit) {
        return unit.convert(keepAliveTime, this.unit);
    }

    /**
     * Set the keep-alive time.  If the service is already started, the change will take effect immediately.
     *
     * @param keepAliveTime the keep-alive time
     * @param unit the time unit
     */
    public synchronized void setKeepAliveTime(final long keepAliveTime, final TimeUnit unit) {
        if (unit == null) {
            throw new IllegalArgumentException("unit is null");
        }
        this.keepAliveTime = keepAliveTime;
        this.unit = unit;
        final ThreadPoolExecutor realExecutor = this.realExecutor;
        if (realExecutor != null) {
            realExecutor.setKeepAliveTime(keepAliveTime, unit);
        }
    }

    /**
     * Set the configured work queue.  If the service is already started, the change will take effect upon next restart.
     *
     * @param workQueue the work queue
     */
    public synchronized void setWorkQueue(final BlockingQueue<Runnable> workQueue) {
        if (workQueue == null) {
            setWorkQueue(new ArrayBlockingQueue<Runnable>(DEFAULT_QUEUE_LENGTH));
        }
        this.workQueue = workQueue;
    }

    /**
     * Get the configured thread factory.
     *
     * @return the thread factory
     */
    public synchronized ThreadFactory getThreadFactory() {
        return threadFactory;
    }

    /**
     * Set the configured thread factory.  If the service is already started, the change will take effect immediately.
     *
     * @param threadFactory the thread factory
     */
    public synchronized void setThreadFactory(final ThreadFactory threadFactory) {
        if (threadFactory == null) {
            setThreadFactory(Executors.defaultThreadFactory());
            return;
        }
        this.threadFactory = threadFactory;
        final ThreadPoolExecutor realExecutor = this.realExecutor;
        if (realExecutor != null) {
            realExecutor.setThreadFactory(threadFactory);
        }
    }

    /**
     * Get the rejected execution handler.
     *
     * @return the rejected execution handler
     */
    public synchronized RejectedExecutionHandler getHandler() {
        return handler;
    }

    /**
     * Set the rejected execution handler.  If the service is already started, the change will take effect immediately.
     *
     * @param handler the rejected execution handler
     */
    public synchronized void setHandler(final RejectedExecutionHandler handler) {
        if (handler == null) {
            throw new IllegalArgumentException("handler is null");
        }
        this.handler = handler;
        final ThreadPoolExecutor realExecutor = this.realExecutor;
        if (realExecutor != null) {
            realExecutor.setRejectedExecutionHandler(handler);
        }
    }

    private ExecutorService publicExecutor;
    private ThreadPoolExecutor realExecutor;
    private StopContext stopContext;

    /**
     * Get the public executor service for this thread pool.
     *
     * @return the public executor service
     *
     * @throws IllegalStateException if the service is not started
     */
    public synchronized ExecutorService getValue() throws IllegalStateException {
        final ExecutorService publicExecutor = this.publicExecutor;
        if (publicExecutor == null) {
            throw new IllegalStateException();
        }
        return publicExecutor;
    }

    /** {@inheritDoc} */
    public synchronized void start(final StartContext context) throws StartException {
        realExecutor = new OurExecutor(corePoolSize, maximumPoolSize, keepAliveTime, unit, workQueue, threadFactory, handler);
        realExecutor.allowCoreThreadTimeOut(allowCoreTimeout);
        publicExecutor = JBossExecutors.protectedExecutorService(realExecutor);
    }

    /** {@inheritDoc} */
    public synchronized void stop(final StopContext context) {
        stopContext = context;
        context.asynchronous();
        realExecutor.shutdown();
    }

    private final class OurExecutor extends ThreadPoolExecutor {

        OurExecutor(final int corePoolSize, final int maximumPoolSize, final long keepAliveTime, final TimeUnit unit, final BlockingQueue<Runnable> workQueue, final ThreadFactory threadFactory, final RejectedExecutionHandler handler) {
            super(corePoolSize, maximumPoolSize, keepAliveTime, unit, workQueue, threadFactory, handler);
        }

        protected void terminated() {
            synchronized (ThreadPoolExecutorService.this) {
                final StopContext context = stopContext;
                if (context != null) {
                    context.complete();
                    stopContext = null;
                    publicExecutor = null;
                    realExecutor = null;
                }
            }
        }
    }
}