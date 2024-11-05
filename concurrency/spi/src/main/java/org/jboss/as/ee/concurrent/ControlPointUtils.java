/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jboss.as.ee.concurrent;

import org.jboss.as.controller.ControlledProcessState;
import org.jboss.as.controller.ProcessStateNotifier;
import org.jboss.as.ee.logging.EeLogger;
import org.wildfly.extension.requestcontroller.ControlPoint;
import org.wildfly.extension.requestcontroller.RunResult;

import jakarta.enterprise.concurrent.ManagedExecutorService;
import jakarta.enterprise.concurrent.ManagedTask;
import jakarta.enterprise.concurrent.ManagedTaskListener;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.concurrent.Future;
import java.util.concurrent.RejectedExecutionException;

import static org.jboss.as.ee.logging.EeLogger.ROOT_LOGGER;

/**
 * Class that manages the control point for executor services
 *
 * @author Stuart Douglas
 */
public class ControlPointUtils {

    public static Runnable doWrap(Runnable runnable, ControlPoint controlPoint, ProcessStateNotifier processStateNotifier) {
        if (controlPoint == null || runnable == null) {
            return runnable;
        }
        final RunResult result;
        try {
            if (processStateNotifier.getCurrentState() == ControlledProcessState.State.STARTING) {
                result = controlPoint.forceBeginRequest();
            } else {
                result = controlPoint.beginRequest();
            }
        } catch (Exception e) {
            throw new RejectedExecutionException(e);
        }
        if (result == RunResult.REJECTED) {
            throw ROOT_LOGGER.rejectedDueToMaxRequests();
        }
        try {
            final ControlledRunnable controlledRunnable = new ControlledRunnable(runnable, controlPoint);
            return runnable instanceof ManagedTask ? new ControlledManagedRunnable(controlledRunnable, (ManagedTask) runnable) : controlledRunnable;
        } catch (Exception e) {
            controlPoint.requestComplete();
            throw new RejectedExecutionException(e);
        }
    }

    public static <T> Callable<T> doWrap(Callable<T> callable, ControlPoint controlPoint, ProcessStateNotifier processStateNotifier) {
        if (controlPoint == null || callable == null) {
            return callable;
        }
        final RunResult result;
        try {
            if (processStateNotifier.getCurrentState() == ControlledProcessState.State.STARTING) {
                result = controlPoint.forceBeginRequest();
            } else {
                result = controlPoint.beginRequest();
            }
        } catch (Exception e) {
            throw new RejectedExecutionException(e);
        }
        if (result == RunResult.REJECTED) {
            throw ROOT_LOGGER.rejectedDueToMaxRequests();
        }
        try {
            final ControlledCallable<T> controlledCallable = new ControlledCallable<>(callable, controlPoint);
            return callable instanceof ManagedTask ? new ControlledManagedCallable<>(controlledCallable, (ManagedTask) callable) : controlledCallable;
        } catch (Exception e) {
            controlPoint.requestComplete();
            throw new RejectedExecutionException(e);
        }
    }

    public static Runnable doScheduledWrap(Runnable runnable, ControlPoint controlPoint, ProcessStateNotifier processStateNotifier) {
        if (controlPoint == null || runnable == null) {
            return runnable;
        } else {
            final ControlledScheduledRunnable controlledScheduledRunnable = new ControlledScheduledRunnable(runnable, controlPoint, processStateNotifier);
            return runnable instanceof ManagedTask ? new ControlledManagedRunnable(controlledScheduledRunnable, (ManagedTask) runnable) : controlledScheduledRunnable;
        }
    }

    public static <T> Callable<T> doScheduledWrap(Callable<T> callable, ControlPoint controlPoint, ProcessStateNotifier processStateNotifier) {
        if (controlPoint == null || callable == null) {
            return callable;
        } else {
            final ControlledScheduledCallable<T> controlledScheduledCallable = new ControlledScheduledCallable<>(callable, controlPoint, processStateNotifier);
            return callable instanceof ManagedTask ? new ControlledManagedCallable<>(controlledScheduledCallable, (ManagedTask) callable) : controlledScheduledCallable;
        }
    }

    /**
     * Runnable that wraps a runnable to allow server suspend/resume to work correctly.
     *
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
     * Runnable that wraps a runnable to be scheduled, which allows server suspend/resume to work correctly.
     *
     */
    static class ControlledScheduledRunnable implements Runnable {

        private final Runnable runnable;
        private final ControlPoint controlPoint;
        private final ProcessStateNotifier processStateNotifier;

        ControlledScheduledRunnable(Runnable runnable, ControlPoint controlPoint, ProcessStateNotifier processStateNotifier) {
            this.runnable = runnable;
            this.controlPoint = controlPoint;
            this.processStateNotifier = processStateNotifier;
        }

        @Override
        public void run() {
            if (controlPoint == null) {
                runnable.run();
            } else {
                RuntimeException runnableException = null;
                try {
                    final RunResult result;
                    if (processStateNotifier.getCurrentState() == ControlledProcessState.State.STARTING) {
                        result = controlPoint.forceBeginRequest();
                    } else {
                        result = controlPoint.beginRequest();
                    }
                    if (result == RunResult.RUN) {
                        try {
                            runnable.run();
                        } catch (RuntimeException e) {
                            runnableException = e;
                            throw e;
                        } finally {
                            controlPoint.requestComplete();
                        }
                    } else {
                        throw EeLogger.ROOT_LOGGER.cannotRunScheduledTask(runnable);
                    }
                } catch (RuntimeException re) {
                    // WFLY-13043
                    if (runnableException == null) {
                        EeLogger.ROOT_LOGGER.failedToRunTask(runnable,re);
                        return;
                    } else {
                        throw EeLogger.ROOT_LOGGER.failureWhileRunningTask(runnable, re);
                    }
                }
            }
        }
    }

    /**
     * Runnable that wraps a callable to be scheduled, which allows server suspend/resume to work correctly.
     *
     */
    static class ControlledScheduledCallable<T> implements Callable<T> {

        private final Callable<T> callable;
        private final ControlPoint controlPoint;
        private final ProcessStateNotifier processStateNotifier;

        ControlledScheduledCallable(Callable<T> callable, ControlPoint controlPoint, ProcessStateNotifier processStateNotifier) {
            this.callable = callable;
            this.controlPoint = controlPoint;
            this.processStateNotifier = processStateNotifier;
        }

        @Override
        public T call() throws Exception {
            if (controlPoint == null) {
                return callable.call();
            } else  {
                try {
                    final RunResult result;
                    if (processStateNotifier.getCurrentState() == ControlledProcessState.State.STARTING) {
                        result = controlPoint.forceBeginRequest();
                    } else {
                        result = controlPoint.beginRequest();
                    }
                    if (result == RunResult.RUN) {
                        try {
                            return callable.call();
                        } finally {
                            controlPoint.requestComplete();
                        }
                    } else {
                        throw EeLogger.ROOT_LOGGER.cannotRunScheduledTask(callable);
                    }
                } catch (Exception e) {
                    throw EeLogger.ROOT_LOGGER.failureWhileRunningTask(callable, e);
                }
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
