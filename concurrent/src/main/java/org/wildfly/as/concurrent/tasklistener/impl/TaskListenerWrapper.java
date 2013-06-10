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

import org.wildfly.as.concurrent.ConcurrentLogger;
import org.wildfly.as.concurrent.tasklistener.TaskListener;

import java.util.concurrent.CancellationException;
import java.util.concurrent.Future;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Wrapper for {@link TaskListener}, responsible for doing the callbacks.
 *
 * @author Eduardo Martins
 */
abstract class TaskListenerWrapper {

    private TaskListener taskListener;
    private final boolean periodic;
    private Future<?> future;

    private final CallbacksLock callbacksLock = new CallbacksLock();

    TaskListenerWrapper(TaskListener taskListener, boolean periodic) {
        this.taskListener = taskListener;
        this.periodic = periodic;
    }

    void taskSubmitted(Future<?> future) {
        if (callbacksLock.lock()) {
            this.future = future;
            try {
                taskListener.taskSubmitted(future);
            } catch (Throwable t) {
                ConcurrentLogger.ROOT_LOGGER.failureInTaskSubmittedCallback(t);
            }
            callbacksLock.unlock();
            if (future.isCancelled()) {
                // a concurrent cancel may have not obtained the callbacks lock...
                taskCancelled();
            }
        }
    }

    void beforeExecution() {
        if (callbacksLock.lock()) {
            try {
                taskListener.taskStarting();
            } catch (Throwable t) {
                ConcurrentLogger.ROOT_LOGGER.failureInTaskStartingCallback(t);
            }
            callbacksLock.unlock();
            if (future.isCancelled()) {
                // a concurrent cancel may have not obtained the callbacks lock...
                taskCancelled();
            }
        }
    }

    void afterExecution(Throwable throwable) {
        if (callbacksLock.lock()) {
            if (future.isCancelled()) {
                try {
                    taskListener.taskDone(new CancellationException());
                } catch (Throwable t) {
                    ConcurrentLogger.ROOT_LOGGER.failureInTaskDoneCallback(t);
                }
                // do not unlock callbacks to avoid possible concurrent callbacks
            } else {
                try {
                    taskListener.taskDone(throwable);
                } catch (Throwable t) {
                    ConcurrentLogger.ROOT_LOGGER.failureInTaskDoneCallback(t);
                }
                if (throwable == null && periodic) {
                    // only unlock callbacks if there is another run
                    callbacksLock.unlock();
                    taskSubmitted(future);
                }
            }
        }
    }

    void taskCancelled() {
        if (callbacksLock.lock()) {
            if (future != null) {
                try {
                    taskListener.taskDone(new CancellationException());
                } catch (Throwable t) {
                    ConcurrentLogger.ROOT_LOGGER.failureInTaskDoneCallback(t);
                }
            }
            // do not unlock callbacks to avoid possible concurrent callbacks
        }
    }

    void taskSubmitFailed(Throwable throwable) {
        if (callbacksLock.lock()) {
            if (future != null) {
                try {
                    taskListener.taskDone(throwable);
                } catch (Throwable t) {
                    ConcurrentLogger.ROOT_LOGGER.failureInTaskDoneCallback(t);
                }
            }
            // do not unlock callbacks to avoid possible concurrent callbacks
        }
    }

    /**
     * A lock to avoid issues with concurrent callbacks
     */
    private static class CallbacksLock {

        private final AtomicBoolean lock = new AtomicBoolean(false);

        private boolean lock() {
            return lock.compareAndSet(false, true);
        }

        private void unlock() {
            lock.set(false);
        }
    }
}