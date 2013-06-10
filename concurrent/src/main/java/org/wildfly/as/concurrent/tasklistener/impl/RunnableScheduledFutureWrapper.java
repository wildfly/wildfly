/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2013, Red Hat, Inc., and individual contributors
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

package org.wildfly.as.concurrent.tasklistener.impl;

import java.util.concurrent.Delayed;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.RunnableScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

/**
 * Wrapper for {@link java.util.concurrent.RunnableScheduledFuture}, responsible for doing the {@link TaskListenerWrapper#taskSubmitted(java.util.concurrent.Future)} callback on instance creation, and {@link org.wildfly.as.concurrent.tasklistener.impl.TaskListenerWrapper#taskCancelled()} callbacks on successful cancels.
 *
 * @author Eduardo Martins
 */
class RunnableScheduledFutureWrapper<V> implements RunnableScheduledFuture<V> {

    final RunnableScheduledFuture<V> runnableScheduledFuture;
    final TaskListenerWrapper taskListenerWrapper;

    RunnableScheduledFutureWrapper(RunnableScheduledFuture<V> runnableScheduledFuture, TaskListenerWrapper taskListenerWrapper) {
        this.runnableScheduledFuture = runnableScheduledFuture;
        this.taskListenerWrapper = taskListenerWrapper;
        taskListenerWrapper.taskSubmitted(this);
    }

    @Override
    public boolean isPeriodic() {
        return runnableScheduledFuture.isPeriodic();
    }

    @Override
    public void run() {
        runnableScheduledFuture.run();
    }

    @Override
    public boolean cancel(boolean mayInterruptIfRunning) {
        if (runnableScheduledFuture.cancel(mayInterruptIfRunning)) {
            taskListenerWrapper.taskCancelled();
            return true;
        } else {
            return false;
        }
    }

    @Override
    public boolean isCancelled() {
        return runnableScheduledFuture.isCancelled();
    }

    @Override
    public boolean isDone() {
        return runnableScheduledFuture.isDone();
    }

    @Override
    public V get() throws InterruptedException, ExecutionException {
        return runnableScheduledFuture.get();
    }

    @Override
    public V get(long timeout, TimeUnit unit) throws InterruptedException, ExecutionException, TimeoutException {
        return runnableScheduledFuture.get(timeout, unit);
    }

    @Override
    public long getDelay(TimeUnit unit) {
        return runnableScheduledFuture.getDelay(unit);
    }

    @Override
    public int compareTo(Delayed o) {
        return runnableScheduledFuture.compareTo(o);
    }
}
