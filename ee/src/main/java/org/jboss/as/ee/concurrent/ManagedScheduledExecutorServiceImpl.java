/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2014, Red Hat, Inc., and individual contributors
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
package org.jboss.as.ee.concurrent;

import org.glassfish.enterprise.concurrent.ContextServiceImpl;
import org.glassfish.enterprise.concurrent.ManagedThreadFactoryImpl;
import org.jboss.as.ee.logging.EeLogger;
import org.wildfly.extension.requestcontroller.ControlPoint;
import org.wildfly.extension.requestcontroller.RunResult;

import javax.enterprise.concurrent.LastExecution;
import javax.enterprise.concurrent.Trigger;
import java.util.Date;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

import static org.jboss.as.ee.concurrent.ControlPointUtils.doWrap;

/**
 * WildFly's extension of Java EE 7 RI {@link org.glassfish.enterprise.concurrent.ManagedScheduledExecutorServiceImpl}.
 *
 * @author Eduardo Martins
 */
public class ManagedScheduledExecutorServiceImpl extends org.glassfish.enterprise.concurrent.ManagedScheduledExecutorServiceImpl {

    private final ControlPoint controlPoint;

    public ManagedScheduledExecutorServiceImpl(String name, ManagedThreadFactoryImpl managedThreadFactory, long hungTaskThreshold, boolean longRunningTasks, int corePoolSize, long keepAliveTime, TimeUnit keepAliveTimeUnit, long threadLifeTime, ContextServiceImpl contextService, RejectPolicy rejectPolicy, ControlPoint controlPoint) {
        super(name, managedThreadFactory, hungTaskThreshold, longRunningTasks, corePoolSize, keepAliveTime, keepAliveTimeUnit, threadLifeTime, contextService, rejectPolicy);
        this.controlPoint = controlPoint;
    }

    @Override
    public void execute(Runnable command) {
        super.execute(doWrap(command, controlPoint));
    }

    @Override
    public Future<?> submit(Runnable task) {
        return super.submit(doWrap(task, controlPoint));
    }

    @Override
    public <T> Future<T> submit(Runnable task, T result) {
        return super.submit(doWrap(task, controlPoint), result);
    }

    @Override
    public <T> Future<T> submit(Callable<T> task) {
        return super.submit(doWrap(task, controlPoint));
    }

    @Override
    public ScheduledFuture<?> schedule(Runnable command, Trigger trigger) {
        final CancellableTrigger ctrigger = new CancellableTrigger(trigger);
        ctrigger.future = super.schedule(new ControlledScheduledRunnable(command, controlPoint, this), ctrigger);
        return ctrigger.future;
    }

    @Override
    public <V> ScheduledFuture<V> schedule(Callable<V> callable, Trigger trigger) {
        final CancellableTrigger ctrigger = new CancellableTrigger(trigger);
        ctrigger.future = super.schedule(new ControlledScheduledCallable(callable, controlPoint), ctrigger);
        return ctrigger.future;
    }

    @Override
    public ScheduledFuture<?> schedule(Runnable command, long delay, TimeUnit unit) {
        return super.schedule(new ControlledScheduledRunnable(command, controlPoint, this), delay, unit);
    }

    @Override
    public <V> ScheduledFuture<V> schedule(Callable<V> callable, long delay, TimeUnit unit) {
        return super.schedule(new ControlledScheduledCallable(callable, controlPoint), delay, unit);
    }

    @Override
    public ScheduledFuture<?> scheduleAtFixedRate(Runnable command, long initialDelay, long period, TimeUnit unit) {
        return super.scheduleAtFixedRate(new ControlledScheduledRunnable(command, controlPoint, this), initialDelay, period, unit);
    }

    @Override
    public ScheduledFuture<?> scheduleWithFixedDelay(Runnable command, long initialDelay, long delay, TimeUnit unit) {
        return super.scheduleWithFixedDelay(new ControlledScheduledRunnable(command, controlPoint, this), initialDelay, delay, unit);
    }

    /**
     * A {@link javax.enterprise.concurrent.Trigger} wrapper that stops scheduling if the related {@link java.util.concurrent.ScheduledFuture} is cancelled.
     */
    private static class CancellableTrigger implements Trigger {
        private final Trigger trigger;
        private ScheduledFuture future;

        CancellableTrigger(Trigger trigger) {
            this.trigger = trigger;
        }

        @Override
        public Date getNextRunTime(LastExecution lastExecution, Date taskScheduledTime) {
            Date nextRunTime = trigger.getNextRunTime(lastExecution, taskScheduledTime);
            final ScheduledFuture future = this.future;
            if (future != null && future.isCancelled()) {
                nextRunTime = null;
            }
            return nextRunTime;
        }

        @Override
        public boolean skipRun(LastExecution lastExecution, Date date) {
            return trigger.skipRun(lastExecution, date);
        }
    }

    private static class ControlledScheduledRunnable implements Runnable {

        private final Runnable delegate;
        private final ControlPoint controlPoint;
        private final ExecutorService executorService;

        private ControlledScheduledRunnable(Runnable delegate, ControlPoint controlPoint, ExecutorService executorService) {
            this.delegate = delegate;
            this.controlPoint = controlPoint;
            this.executorService = executorService;
        }

        @Override
        public void run() {
            if (controlPoint == null) {
                delegate.run();
            } else
                try {
                    if (controlPoint.beginRequest() == RunResult.RUN) {
                        try {
                            delegate.run();
                        } finally {
                            controlPoint.requestComplete();
                        }
                        return;
                    } else {
                        throw EeLogger.ROOT_LOGGER.cannotRunScheduledTask(delegate);
                    }
                } catch (Exception e) {
                    EeLogger.ROOT_LOGGER.failedToRunTask(e);
                }
        }
    }


    private static class ControlledScheduledCallable<V> implements Callable<V> {

        private final Callable<V> delegate;
        private final ControlPoint controlPoint;

        private ControlledScheduledCallable(Callable<V> delegate, ControlPoint controlPoint) {
            this.delegate = delegate;
            this.controlPoint = controlPoint;
        }

        @Override
        public V call() throws Exception {
            if (controlPoint == null) {
                return delegate.call();
            } else  {
                try {
                    if (controlPoint.beginRequest() == RunResult.RUN) {
                        try {
                            return delegate.call();
                        } finally {
                            controlPoint.requestComplete();
                        }
                    }
                } catch (Exception e) {
                    EeLogger.ROOT_LOGGER.failedToRunTask(e);
                }
                throw EeLogger.ROOT_LOGGER.cannotRunScheduledTask(delegate);
            }

        }
    }

}
