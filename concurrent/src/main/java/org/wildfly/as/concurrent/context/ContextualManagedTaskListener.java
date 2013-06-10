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
package org.wildfly.as.concurrent.context;

import javax.enterprise.concurrent.ManagedExecutorService;
import javax.enterprise.concurrent.ManagedTask;
import javax.enterprise.concurrent.ManagedTaskListener;
import java.util.concurrent.Future;

/**
 * A {@link ManagedTaskListener} which invocations are done with a specific {@link Context} set.
 *
 * @author Eduardo Martins
 */
public class ContextualManagedTaskListener implements ManagedTaskListener {

    private final Context context;
    private final ManagedTaskListener listener;

    /**
     * @param context
     * @param listener
     */
    public ContextualManagedTaskListener(Context context, ManagedTaskListener listener) {
        this.context = context;
        this.listener = listener;
    }

    @Override
    public void taskAborted(Future<?> future, ManagedExecutorService executor, Throwable exception) {
        final Context previousContext = Utils.setContext(context);
        try {
            listener.taskAborted(future, executor, exception);
        } finally {
            Utils.setContext(previousContext);
        }
    }

    @Override
    public void taskDone(Future<?> future, ManagedExecutorService executor, Throwable exception) {
        final Context previousContext = Utils.setContext(context);
        try {
            listener.taskDone(future, executor, exception);
        } finally {
            Utils.setContext(previousContext);
        }
    }

    @Override
    public void taskStarting(Future<?> future, ManagedExecutorService executor) {
        final Context previousContext = Utils.setContext(context);
        try {
            listener.taskStarting(future, executor);
        } finally {
            Utils.setContext(previousContext);
        }
    }

    @Override
    public void taskSubmitted(Future<?> future, ManagedExecutorService executor) {
        final Context previousContext = Utils.setContext(context);
        try {
            listener.taskSubmitted(future, executor);
        } finally {
            Utils.setContext(previousContext);
        }
    }

    /**
     * Retrieves a contextual {@link ManagedTaskListener} for the specified {@link ContextConfiguration}, but only if the specified {@link ManagedTaskConfiguration} indicates that.
     *
     * @param managedTaskConfiguration
     * @param contextConfiguration
     * @return
     */
    public static ManagedTaskListener getContextualManagedTaskListener(ManagedTaskConfiguration managedTaskConfiguration, ContextConfiguration contextConfiguration) {
        final ManagedTask managedTask = managedTaskConfiguration.getManagedTask();
        final ManagedTaskListener managedTaskListener = managedTask.getManagedTaskListener();
        if (managedTaskListener == null || contextConfiguration == null) {
            return managedTaskListener;
        }
        if (managedTaskConfiguration.isManagedTaskWithContextualCallbacks()) {
            return new ContextualManagedTaskListener(contextConfiguration.newManagedTaskListenerContext(managedTaskListener), managedTaskListener);
        } else {
            return managedTaskListener;
        }
    }

}
