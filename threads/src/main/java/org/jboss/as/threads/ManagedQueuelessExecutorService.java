/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2011, Red Hat, Inc., and individual contributors
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

import java.util.concurrent.ExecutorService;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.TimeUnit;

import org.jboss.threads.BlockingExecutor;
import org.jboss.threads.EventListener;
import org.jboss.threads.JBossExecutors;
import org.jboss.threads.QueuelessExecutor;

/**
 *
 * @author Alexey Loubyansky
 */
public class ManagedQueuelessExecutorService extends ManagedExecutorService implements BlockingExecutor {

    private final QueuelessExecutor executor;

    public ManagedQueuelessExecutorService(QueuelessExecutor executor) {
        super(executor);
        this.executor = executor;
    }

    @Override
    protected ExecutorService protectExecutor(ExecutorService executor) {
        return JBossExecutors.protectedBlockingExecutorService((BlockingExecutor) executor);
    }

    @Override
    void internalShutdown() {
        executor.shutdown();
    }

    public boolean isBlocking() {
        return executor.isBlocking();
    }

    void setBlocking(boolean blocking) {
        executor.setBlocking(blocking);
    }

    public int getMaxThreads() {
        return executor.getMaxThreads();
    }

    void setMaxThreads(int maxThreads) {
        executor.setMaxThreads(maxThreads);
    }

    public long getKeepAlive() {
        return executor.getKeepAliveTime();
    }

    void setKeepAlive(long milliseconds) {
        executor.setKeepAliveTime(milliseconds);
    }

    public int getRejectedCount() {
        return executor.getRejectedCount();
    }

    public int getCurrentThreadCount() {
        return executor.getCurrentThreadCount();
    }

    public int getLargestThreadCount() {
        return executor.getLargestThreadCount();
    }

    <A> void addShutdownListener(final EventListener<A> shutdownListener, final A attachment) {
        executor.addShutdownListener(shutdownListener, attachment);
    }

    @Override
    public void executeBlocking(Runnable task)
            throws RejectedExecutionException, InterruptedException {
        executor.executeBlocking(task);
    }

    @Override
    public void executeBlocking(Runnable task, long timeout, TimeUnit unit)
            throws RejectedExecutionException, InterruptedException {
        executor.executeBlocking(task, timeout, unit);
    }

    @Override
    public void executeNonBlocking(Runnable task)
            throws RejectedExecutionException {
        executor.executeNonBlocking(task);
    }
}
