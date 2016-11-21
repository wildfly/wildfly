/*
 * JBoss, Home of Professional Open Source
 * Copyright 2015, Red Hat, Inc., and individual contributors
 * by the @authors tag. See the copyright.txt in the distribution for a
 * full listing of individual contributors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * http://www.apache.org/licenses/LICENSE-2.0
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.jboss.as.weld.services.bootstrap;

import java.security.AccessController;
import java.security.PrivilegedAction;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;

import org.jboss.as.server.Services;
import org.jboss.msc.service.Service;
import org.jboss.msc.service.ServiceName;
import org.jboss.msc.service.StartContext;
import org.jboss.msc.service.StartException;
import org.jboss.msc.service.StopContext;
import org.jboss.threads.JBossThreadFactory;
import org.jboss.weld.executor.AbstractExecutorServices;
import org.jboss.weld.manager.api.ExecutorServices;
import org.wildfly.security.manager.WildFlySecurityManager;

/**
 * Weld's ExecutorServices implementation. The executor is shared across all CDI-enabled deployments and used primarily for parallel Weld bootstrap.
 *
 * @author Jozef Hartinger
 *
 */
public class WeldExecutorServices extends AbstractExecutorServices implements Service<ExecutorServices> {

    public static final ServiceName SERVICE_NAME = Services.JBOSS_AS.append("weld", "executor");
    private static final String THREAD_NAME_PATTERN = "Weld Thread Pool -- %t";

    private final int bound;
    private ExecutorService executor;

    public WeldExecutorServices() {
        this.bound = Runtime.getRuntime().availableProcessors() + 1;
    }

    @Override
    public void start(StartContext context) throws StartException {
        final ThreadGroup threadGroup = new ThreadGroup("Weld ThreadGroup");
        final ThreadFactory factory = new JBossThreadFactory(threadGroup, Boolean.FALSE, null, THREAD_NAME_PATTERN, null, null);
        // set TCCL to null for new threads to make sure no deployment classloader leaks through this executor's TCCL
        // Weld does not mind having null TCCL in this executor
        this.executor = Executors.newFixedThreadPool(bound, runnable -> {
            Thread thread = factory.newThread(runnable);
            if (WildFlySecurityManager.isChecking()) {
                AccessController.doPrivileged(new PrivilegedAction<Void>() {
                    public Void run() {
                        thread.setContextClassLoader(null);
                        return null;
                    }
                });
            } else {
                thread.setContextClassLoader(null);
            }
            return thread;
        }
        );
    }

    @Override
    public void stop(StopContext context) {
        if (executor != null) {
            context.asynchronous();
            new Thread(() -> {
                super.cleanup();
                executor = null;
                context.complete();
            }).start();
        }
    }

    @Override
    protected synchronized int getThreadPoolSize() {
        return bound;
    }

    public synchronized ExecutorServices getValue() throws IllegalStateException, IllegalArgumentException {
        return this;
    }

    @Override
    public synchronized ExecutorService getTaskExecutor() {
        return executor;
    }

    @Override
    public void cleanup() {
        // noop on undeploy - the executor is a service shared across multiple deployments
    }
}
