/*
 * Copyright 2016 Red Hat, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.jboss.as.ee.concurrent.service;

import java.util.Deque;
import java.util.concurrent.Executor;
import java.util.concurrent.LinkedBlockingDeque;
import java.util.concurrent.atomic.AtomicBoolean;
import javax.enterprise.concurrent.ManagedTask;

import org.jboss.as.ee.concurrent.ControlPointUtils;
import org.jboss.as.ee.concurrent.ControlledTaskManager;
import org.jboss.as.ee.logging.EeLogger;
import org.jboss.as.server.suspend.ServerActivity;
import org.jboss.as.server.suspend.ServerActivityCallback;
import org.jboss.as.server.suspend.SuspendController;
import org.jboss.msc.service.Service;
import org.jboss.msc.service.StartContext;
import org.jboss.msc.service.StartException;
import org.jboss.msc.service.StopContext;
import org.jboss.msc.value.InjectedValue;
import org.wildfly.extension.requestcontroller.ControlPoint;
import org.wildfly.extension.requestcontroller.RequestController;
import org.wildfly.extension.requestcontroller.RunResult;

/**
 * A service which implements the {@link ControlledTaskManager}.
 *
 * @author <a href="mailto:jperkins@redhat.com">James R. Perkins</a>
 */
public class ControlledTaskManagerService implements Service<ControlledTaskManager>, ControlledTaskManager {
    private final InjectedValue<SuspendController> suspendControllerInjector = new InjectedValue<>();
    private final InjectedValue<RequestController> requestControllerInjector = new InjectedValue<>();

    private final String entryPointName;
    private final String name;
    private final AtomicBoolean serverSuspended = new AtomicBoolean(false);
    private final Deque<QueuedTask> taskQueue = new LinkedBlockingDeque<>();
    private ServerActivity serverActivity;
    private ControlPoint controlPoint;

    /**
     * Creates a new service.
     *
     * @param entryPointName the entry point name for the control point
     * @param name           the name used for the deployment name of the control point
     */
    public ControlledTaskManagerService(final String entryPointName, final String name) {
        this.entryPointName = entryPointName;
        this.name = name;
    }

    @Override
    public synchronized void start(final StartContext context) throws StartException {
        final ServerActivity serverActivity = new ConcurrentServerActivity();
        suspendControllerInjector.getValue().registerActivity(serverActivity);
        final RequestController requestController = requestControllerInjector.getOptionalValue();
        if (requestController != null) {
            controlPoint = requestController.getControlPoint(name, entryPointName);
        }
        this.serverActivity = serverActivity;
    }

    @Override
    public synchronized void stop(final StopContext context) {
        taskQueue.clear();
        if (serverActivity != null) {
            suspendControllerInjector.getValue().unRegisterActivity(serverActivity);
        }
        if (controlPoint != null) {
            requestControllerInjector.getValue().removeControlPoint(controlPoint);
        }
    }

    @Override
    public ControlledTaskManager getValue() throws IllegalStateException, IllegalArgumentException {
        return this;
    }

    @Override
    public synchronized ControlPoint getControlPoint() {
        return controlPoint;
    }

    @Override
    public void submitTask(final Runnable task, final Executor executor) {
        if (getControlPoint() == null) {
            executor.execute(task);
        } else {
            // If the server is suspended the task should be queued, otherwise it can be submitted
            final QueuedTask queuedTask = new QueuedTask(task, executor);
            if (serverSuspended.get()) {
                queueTask(queuedTask);
            } else {
                queuedTask.submit();
            }
        }
    }

    @Override
    public boolean isSuspended() {
        return serverSuspended.get();
    }

    public InjectedValue<SuspendController> getSuspendControllerInjector() {
        return suspendControllerInjector;
    }

    public InjectedValue<RequestController> getRequestControllerInjector() {
        return requestControllerInjector;
    }

    private void queueTask(final QueuedTask task) {
        if (task.queued.compareAndSet(false, true)) {
            taskQueue.addLast(task);
        }
    }

    private class ConcurrentServerActivity implements ServerActivity {


        @Override
        public void preSuspend(final ServerActivityCallback serverActivityCallback) {
            serverActivityCallback.done();
        }

        @Override
        public void suspended(final ServerActivityCallback serverActivityCallback) {
            serverSuspended.set(true);
            serverActivityCallback.done();
        }

        @Override
        public void resume() {
            // Process tasks that have been queued while the server was suspended
            if (serverSuspended.compareAndSet(true, false)) {
                QueuedTask task;
                while ((task = taskQueue.poll()) != null) {
                    task.submit();
                }
            }
        }
    }

    private class QueuedTask implements Runnable {
        final Runnable runnable;
        final Executor executor;
        final AtomicBoolean queued;

        private QueuedTask(final Runnable runnable, final Executor executor) {
            this.runnable = runnable;
            this.executor = executor;
            queued = new AtomicBoolean(false);
        }

        @Override
        public void run() {
            final ControlPoint controlPoint = getControlPoint();
            try {
                if (controlPoint.beginRequest() == RunResult.RUN) {
                    runnable.run();
                } else {
                    queueTask(this);
                }
            } catch (Exception e) {
                EeLogger.ROOT_LOGGER.failedToRunTask(e);
            } finally {
                controlPoint.requestComplete();
            }
        }

        void submit() {
            Runnable task = runnable;
            if (task instanceof ManagedTask) {
                task = ControlPointUtils.doWrapMangedTask(this, (ManagedTask) runnable);
            }
            executor.execute(task);
        }
    }
}