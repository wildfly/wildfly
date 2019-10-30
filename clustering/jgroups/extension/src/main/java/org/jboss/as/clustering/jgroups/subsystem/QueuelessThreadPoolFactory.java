/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2018, Red Hat, Inc., and individual contributors
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

package org.jboss.as.clustering.jgroups.subsystem;

import java.util.concurrent.Executor;
import java.util.concurrent.RejectedExecutionHandler;
import java.util.concurrent.SynchronousQueue;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import org.jgroups.util.ShutdownRejectedExecutionHandler;

/**
 * Factory for creating a queue-less thread pool.
 * @author Paul Ferraro
 */
public class QueuelessThreadPoolFactory implements ThreadPoolConfiguration, ThreadPoolFactory {

    private volatile int minThreads = 0;
    private volatile int maxThreads = Integer.MAX_VALUE;
    private volatile long keepAliveTime = 0;

    public QueuelessThreadPoolFactory setMinThreads(int minThreads) {
        this.minThreads = minThreads;
        return this;
    }

    public QueuelessThreadPoolFactory setMaxThreads(int maxThreads) {
        this.maxThreads = maxThreads;
        return this;
    }

    public QueuelessThreadPoolFactory setKeepAliveTime(long keepAliveTime) {
        this.keepAliveTime = keepAliveTime;
        return this;
    }

    @Override
    public int getMinThreads() {
        return this.minThreads;
    }

    @Override
    public int getMaxThreads() {
        return this.maxThreads;
    }

    @Override
    public long getKeepAliveTime() {
        return this.keepAliveTime;
    }

    @Override
    public Executor apply(ThreadFactory threadFactory) {
        RejectedExecutionHandler handler = new ShutdownRejectedExecutionHandler(new ThreadPoolExecutor.AbortPolicy());
        return new ThreadPoolExecutor(this.getMinThreads(), this.getMaxThreads(), this.getKeepAliveTime(), TimeUnit.MILLISECONDS, new SynchronousQueue<>(), threadFactory, handler);
    }
}
