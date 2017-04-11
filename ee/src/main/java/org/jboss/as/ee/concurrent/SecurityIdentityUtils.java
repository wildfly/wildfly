/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2017 Red Hat, Inc., and individual contributors
 * as indicated by the @author tags.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.jboss.as.ee.concurrent;

import java.security.AccessController;
import java.security.PrivilegedAction;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.concurrent.Future;

import javax.enterprise.concurrent.ManagedExecutorService;
import javax.enterprise.concurrent.ManagedTask;
import javax.enterprise.concurrent.ManagedTaskListener;

import org.wildfly.security.auth.server.SecurityDomain;
import org.wildfly.security.auth.server.SecurityIdentity;

/**
 * Utilities for capturing the current SecurityIdentity and wrapping tasks.
 *
 * @author <a href="mailto:darran.lofthouse@jboss.com">Darran Lofthouse</a>
 */
class SecurityIdentityUtils {

    private SecurityIdentityUtils() {
    }

    static <T> Callable<T> doIdentityWrap(final Callable<T> callable) {
        if(callable == null) {
            return null;
        }
        final SecurityIdentity securityIdentity = getSecurityIdentity();
        if(securityIdentity == null) {
            return callable;
        }
        Callable<T> securedCallable = () -> securityIdentity.runAs(callable);
        return callable instanceof ManagedTask ? new SecuredManagedCallable<T>(securedCallable, (ManagedTask) callable) : securedCallable;
    }

    static Runnable doIdentityWrap(final Runnable runnable) {
        if(runnable == null) {
            return null;
        }
        final SecurityIdentity securityIdentity = getSecurityIdentity();
        if(securityIdentity == null) {
            return runnable;
        }
        Runnable securedRunnable = () -> securityIdentity.runAs(runnable);
        return runnable instanceof ManagedTask ? new SecuredManagedRunnable(securedRunnable, (ManagedTask) runnable) : securedRunnable;
    }

    private static SecurityIdentity getSecurityIdentity() {
        final SecurityManager sm = System.getSecurityManager();
        final SecurityDomain securityDomain;
        if (sm != null) {
            securityDomain = AccessController.doPrivileged((PrivilegedAction<SecurityDomain>) () -> SecurityDomain.getCurrent());
        } else {
            securityDomain = SecurityDomain.getCurrent();
        }
        return securityDomain != null ? securityDomain.getCurrentSecurityIdentity() : null;
    }

    /**
     * A managed Secured task.
     */
    static class SecuredManagedTask implements ManagedTask {

        private final ManagedTask managedTask;
        private final SecurityIdentityUtils.SecuredManagedTaskListener managedTaskListenerWrapper;

        SecuredManagedTask(ManagedTask managedTask) {
            this.managedTask = managedTask;
            this.managedTaskListenerWrapper = managedTask.getManagedTaskListener() != null ? new SecurityIdentityUtils.SecuredManagedTaskListener(managedTask.getManagedTaskListener()) : null;
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
     * A managed Secured task which is a runnable.
     *
     */
    static class SecuredManagedRunnable extends SecurityIdentityUtils.SecuredManagedTask implements Runnable {

        private final Runnable runnable;

        SecuredManagedRunnable(Runnable SecuredTask, ManagedTask managedTask) {
            super(managedTask);
            this.runnable = SecuredTask;
        }

        @Override
        public void run() {
            runnable.run();
        }
    }

    /**
     * A managed Secured task which is a callable.
     *
     */
    static class SecuredManagedCallable<T> extends SecurityIdentityUtils.SecuredManagedTask implements Callable<T> {

        private final Callable<T> runnable;

        SecuredManagedCallable(Callable<T> SecuredTask, ManagedTask managedTask) {
            super(managedTask);
            this.runnable = SecuredTask;
        }

        @Override
        public T call() throws Exception {
            return runnable.call();
        }
    }

    /**
     * A managed task listener for managed Secured tasks.
     */
    static class SecuredManagedTaskListener implements ManagedTaskListener {

        private final ManagedTaskListener managedTaskListener;

        SecuredManagedTaskListener(ManagedTaskListener managedTaskListener) {
            this.managedTaskListener = managedTaskListener;
        }

        @Override
        public void taskAborted(Future<?> future, ManagedExecutorService executor, Object task, Throwable exception) {
            managedTaskListener.taskAborted(future, executor, ((SecurityIdentityUtils.SecuredManagedTask)task).managedTask, exception);
        }

        @Override
        public void taskDone(Future<?> future, ManagedExecutorService executor, Object task, Throwable exception) {
            managedTaskListener.taskDone(future, executor, ((SecurityIdentityUtils.SecuredManagedTask) task).managedTask, exception);
        }

        @Override
        public void taskStarting(Future<?> future, ManagedExecutorService executor, Object task) {
            managedTaskListener.taskStarting(future, executor, ((SecurityIdentityUtils.SecuredManagedTask) task).managedTask);
        }

        @Override
        public void taskSubmitted(Future<?> future, ManagedExecutorService executor, Object task) {
            managedTaskListener.taskSubmitted(future, executor, ((SecurityIdentityUtils.SecuredManagedTask) task).managedTask);
        }
    }
}
