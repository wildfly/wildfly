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
 * 2110-1301 USA, or see the FSF site: http://www.fsf.org.
 */

package org.jboss.as.ee.concurrent;

import org.wildfly.extension.requestcontroller.ControlPoint;

import javax.enterprise.concurrent.ManagedExecutorService;
import javax.enterprise.concurrent.ManagedTask;
import javax.enterprise.concurrent.ManagedTaskListener;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.concurrent.Future;
import java.util.concurrent.RejectedExecutionException;

/**
 * Class that manages the control point for executor services
 *
 * @author Stuart Douglas
 */
public class ControlPointUtils {

    public static Runnable doWrap(Runnable runnable, ControlPoint controlPoint) {
        if (controlPoint == null || runnable == null) {
            return runnable;
        }
        try {
            controlPoint.forceBeginRequest();
            final ControlledRunnable controlledRunnable = new ControlledRunnable(runnable, controlPoint);
            return runnable instanceof ManagedTask ? new ControlledManagedRunnable(controlledRunnable, (ManagedTask) runnable) : controlledRunnable;
        } catch (Exception e) {
            throw new RejectedExecutionException(e);
        }
    }

    public static <T> Callable<T> doWrap(Callable<T> callable, ControlPoint controlPoint) {
        if (controlPoint == null || callable == null) {
            return callable;
        }
        try {
            controlPoint.forceBeginRequest();
            final ControlledCallable<T> controlledCallable = new ControlledCallable<>(callable, controlPoint);
            return callable instanceof ManagedTask ? new ControlledManagedCallable<>(controlledCallable, (ManagedTask) callable) : controlledCallable;
        } catch (Exception e) {
            throw new RejectedExecutionException(e);
        }
    }

    public static Runnable doWrapMangedTask(final Runnable runnable, final ManagedTask managedTask) {
        return new ControlledManagedRunnable(runnable, managedTask);
    }

    /**
     * Runnable that wraps a runnable to allow server suspend/resume to work correctly.
     */
    static class ControlledRunnable implements Runnable {

        private final Runnable runnable;
        private final ControlPoint controlPoint;

        ControlledRunnable(Runnable runnable, ControlPoint controlPoint) {
            this.runnable = runnable;
            this.controlPoint = controlPoint;
        }

        @Override
        public void run() {
            try {
                runnable.run();
            } finally {
                controlPoint.requestComplete();
            }
        }
    }

    /**
     * Runnable that wraps a callable to allow server suspend/resume to work correctly.
     *
     */
    static class ControlledCallable<T> implements Callable<T> {

        private final Callable<T> callable;
        private final ControlPoint controlPoint;

        ControlledCallable(Callable<T> callable, ControlPoint controlPoint) {
            this.callable = callable;
            this.controlPoint = controlPoint;
        }

        @Override
        public T call() throws Exception {
            try {
                return callable.call();
            } finally {
                controlPoint.requestComplete();
            }
        }
    }

    /**
     * A managed controlled task.
     */
    static class ControlledManagedTask implements ManagedTask {

        private final ManagedTask managedTask;
        private final ControlledManagedTaskListener managedTaskListenerWrapper;

        ControlledManagedTask(ManagedTask managedTask) {
            this.managedTask = managedTask;
            this.managedTaskListenerWrapper = managedTask.getManagedTaskListener() != null ? new ControlledManagedTaskListener(managedTask.getManagedTaskListener()) : null;
        }

        @Override
        public Map<String, String> getExecutionProperties() {
            return managedTask.getExecutionProperties();
        }

        @Override
        public ManagedTaskListener getManagedTaskListener() {
            return managedTaskListenerWrapper;
        }
    }

    /**
     * A managed controlled task which is a runnable.
     *
     */
    static class ControlledManagedRunnable extends ControlledManagedTask implements Runnable {

        private final Runnable controlledTask;

        ControlledManagedRunnable(Runnable controlledTask, ManagedTask managedTask) {
            super(managedTask);
            this.controlledTask = controlledTask;
        }

        @Override
        public void run() {
            controlledTask.run();
        }
    }

    /**
     * A managed controlled task which is a callable.
     *
     */
    static class ControlledManagedCallable<T> extends ControlledManagedTask implements Callable<T> {

        private final Callable<T> controlledTask;

        ControlledManagedCallable(Callable<T> controlledTask, ManagedTask managedTask) {
            super(managedTask);
            this.controlledTask = controlledTask;
        }

        @Override
        public T call() throws Exception {
            return controlledTask.call();
        }
    }

    /**
     * A managed task listener for managed controlled tasks.
     */
    static class ControlledManagedTaskListener implements ManagedTaskListener {

        private final ManagedTaskListener managedTaskListener;

        ControlledManagedTaskListener(ManagedTaskListener managedTaskListener) {
            this.managedTaskListener = managedTaskListener;
        }

        @Override
        public void taskAborted(Future<?> future, ManagedExecutorService executor, Object task, Throwable exception) {
            managedTaskListener.taskAborted(future, executor, ((ControlledManagedTask)task).managedTask, exception);
        }

        @Override
        public void taskDone(Future<?> future, ManagedExecutorService executor, Object task, Throwable exception) {
            managedTaskListener.taskDone(future, executor, ((ControlledManagedTask) task).managedTask, exception);
        }

        @Override
        public void taskStarting(Future<?> future, ManagedExecutorService executor, Object task) {
            managedTaskListener.taskStarting(future, executor, ((ControlledManagedTask) task).managedTask);
        }

        @Override
        public void taskSubmitted(Future<?> future, ManagedExecutorService executor, Object task) {
            managedTaskListener.taskSubmitted(future, executor, ((ControlledManagedTask) task).managedTask);
        }
    }
}
