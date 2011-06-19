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
package org.jboss.as.clustering.jgroups;

import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import org.jgroups.util.ThreadDecorator;
import org.jgroups.util.ThreadFactory;
import org.jgroups.util.TimeScheduler;

/**
 * Adapts a {@link ScheduledExecutorService} to a {@link TimeScheduler}.
 * @author Paul Ferraro
 */
public class TimerSchedulerAdapter implements TimeScheduler {

    final ScheduledExecutorService executor;

    public TimerSchedulerAdapter(ScheduledExecutorService executor) {
        this.executor = executor;
    }

    @Override
    public ThreadDecorator getThreadDecorator() {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public void setThreadDecorator(ThreadDecorator decorator) {
        // TODO Auto-generated method stub
    }

    @Override
    public void execute(Runnable command) {
        this.executor.execute(command);
    }

    @Override
    public Future<?> schedule(Runnable command, long delay, TimeUnit unit) {
        return this.executor.schedule(command, delay, unit);
    }

    @Override
    public Future<?> scheduleWithFixedDelay(Runnable command, long initialDelay, long delay, TimeUnit unit) {
        return this.executor.scheduleWithFixedDelay(command, initialDelay, delay, unit);
    }

    @Override
    public Future<?> scheduleAtFixedRate(Runnable command, long initialDelay, long period, TimeUnit unit) {
        return this.executor.scheduleAtFixedRate(command, initialDelay, period, unit);
    }

    @Override
    public Future<?> scheduleWithDynamicInterval(final Task task) {

        final Future<?> future = this.executor.schedule(task, task.nextInterval(), TimeUnit.MILLISECONDS);
        final long nextInterval = task.nextInterval();
        if (nextInterval > 0) {
            Runnable scheduleTask = new Runnable() {
                @Override
                public void run() {
                    try {
                        future.get();
                        long interval = nextInterval;
                        while ((interval > 0) && !future.isCancelled() && !Thread.currentThread().isInterrupted()) {
                            try {
                                TimerSchedulerAdapter.this.executor.schedule(task, interval, TimeUnit.MILLISECONDS).get();
                            } catch (ExecutionException e) {
                            }
                            interval = task.nextInterval();
                        }
                    } catch (InterruptedException e) {
                    } catch (ExecutionException e) {
                    }
                }
            };
            this.execute(scheduleTask);
        }
        return future;
    }

    @Override
    public void setThreadFactory(ThreadFactory factory) {
        this.getExecutor().setThreadFactory(factory);
    }

    @Override
    public String dumpTimerTasks() {
        return this.getExecutor().getQueue().toString();
    }

    @Override
    public int getMinThreads() {
        return this.getExecutor().getCorePoolSize();
    }

    @Override
    public void setMinThreads(int size) {
        this.getExecutor().setCorePoolSize(size);
    }

    @Override
    public int getMaxThreads() {
        return this.getExecutor().getMaximumPoolSize();
    }

    @Override
    public void setMaxThreads(int size) {
        this.getExecutor().setMaximumPoolSize(size);
    }

    @Override
    public long getKeepAliveTime() {
        return this.getExecutor().getKeepAliveTime(TimeUnit.MILLISECONDS);
    }

    @Override
    public void setKeepAliveTime(long time) {
        this.getExecutor().setKeepAliveTime(time, TimeUnit.MILLISECONDS);
    }

    @Override
    public int getCurrentThreads() {
        return this.getExecutor().getActiveCount();
    }

    @Override
    public int size() {
        return this.getExecutor().getPoolSize();
    }

    @Override
    public void stop() {
        this.executor.shutdown();
    }

    @Override
    public boolean isShutdown() {
        return this.executor.isShutdown();
    }

    private ScheduledThreadPoolExecutor getExecutor() {
        if (!(this.executor instanceof ScheduledThreadPoolExecutor)) {
            throw new UnsupportedOperationException();
        }
        return (ScheduledThreadPoolExecutor) this.executor;
    }
}
