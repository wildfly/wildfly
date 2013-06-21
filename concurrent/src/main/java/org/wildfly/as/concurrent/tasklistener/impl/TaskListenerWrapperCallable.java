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

import org.wildfly.as.concurrent.tasklistener.TaskListener;

import java.util.concurrent.Callable;

/**
 * A {@link Callable} {@link TaskListenerWrapper}.
 *
 * @author Eduardo Martins
 */
class TaskListenerWrapperCallable<V> extends TaskListenerWrapper implements Callable<V> {

    private final Callable<V> task;

    TaskListenerWrapperCallable(Callable<V> task, boolean periodic) {
        super((TaskListener) task, periodic);
        this.task = task;
    }

    @Override
    public V call() throws Exception {
        Throwable t = null;
        try {
            beforeExecution();
            return task.call();
        } catch (Error e) {
            t = e;
            throw e;
        } catch (Exception e) {
            t = e;
            throw e;
        } finally {
            afterExecution(t);
        }
    }

    /**
     * Wraps the task, but only if the task is a TaskListener
     *
     * @param task
     * @param periodic
     * @return
     */
    static <V> Callable<V> wrap(Callable<V> task, boolean periodic) {
        return task instanceof TaskListener ? new TaskListenerWrapperCallable<>(task, periodic) : task;
    }
}