/*
 * JBoss, Home of Professional Open Source
 * Copyright 2015, Red Hat Inc., and individual contributors as indicated
 * by the @authors tag. See the copyright.txt in the distribution for a
 * full listing of individual contributors.
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
package org.jboss.as.weld.services.bootstrap;

import java.security.AccessController;
import java.security.PrivilegedAction;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;

import org.jboss.as.naming.context.NamespaceContextSelector;
import org.jboss.as.server.Services;
import org.jboss.msc.Service;
import org.jboss.msc.service.ServiceName;
import org.jboss.msc.service.StartContext;
import org.jboss.msc.service.StartException;
import org.jboss.msc.service.StopContext;
import org.jboss.threads.JBossThreadFactory;
import org.jboss.weld.Container;
import org.jboss.weld.ContainerState;
import org.jboss.weld.executor.AbstractExecutorServices;
import org.jboss.weld.manager.api.ExecutorServices;
import org.wildfly.security.manager.WildFlySecurityManager;

/**
 * Weld's ExecutorServices implementation. The executor is shared across all Jakarta Contexts and Dependency Injection enabled deployments and used primarily for parallel Weld bootstrap.
 *
 * @author Jozef Hartinger
 * @author <a href="mailto:ropalka@redhat.com">Richard Opalka</a>
 */
public class WeldExecutorServices extends AbstractExecutorServices implements Service {

    public static final int DEFAULT_BOUND = Runtime.getRuntime().availableProcessors() + 1;
    public static final ServiceName SERVICE_NAME = Services.JBOSS_AS.append("weld", "executor");
    private static final String THREAD_NAME_PATTERN = "Weld Thread Pool -- %t";

    private final int bound;
    private final Consumer<ExecutorServices> executorServicesConsumer;
    private ExecutorService executor;

    public WeldExecutorServices() {
        this(null, DEFAULT_BOUND);
    }

    public WeldExecutorServices(final Consumer<ExecutorServices> executorServicesConsumer, int bound) {
        this.executorServicesConsumer = executorServicesConsumer;
        this.bound = bound;
    }

    @Override
    public void start(final StartContext context) throws StartException {
        final ThreadGroup threadGroup = new ThreadGroup("Weld ThreadGroup");
        final ThreadFactory factory = new JBossThreadFactory(threadGroup, Boolean.FALSE, null, THREAD_NAME_PATTERN, null, null);
        // set TCCL to null for new threads to make sure no deployment classloader leaks through this executor's TCCL
        // Weld does not mind having null TCCL in this executor
        this.executor = new WeldExecutor(bound, runnable -> {
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
        if (executorServicesConsumer != null) executorServicesConsumer.accept(this);
    }

    @Override
    public void stop(final StopContext context) {
        if (executorServicesConsumer != null) executorServicesConsumer.accept(null);
        if (executor != null) {
            context.asynchronous();
            new Thread(() -> {
                super.shutdown();
                executor = null;
                context.complete();
            }).start();
        }
    }

    @Override
    protected synchronized int getThreadPoolSize() {
        return bound;
    }

    @Override
    public synchronized ExecutorService getTaskExecutor() {
        return executor;
    }

    @Override
    public void cleanup() {
        // noop on undeploy - the executor is a service shared across multiple deployments
    }

    static class WeldExecutor extends ThreadPoolExecutor {

        WeldExecutor(int nThreads, ThreadFactory threadFactory) {
            super(nThreads, nThreads,
                               0L, TimeUnit.MILLISECONDS,
                               new LinkedBlockingQueue<Runnable>(),
                               threadFactory);
        }

        @Override
        protected void beforeExecute(Thread t, Runnable r) {
            if (r instanceof WeldTaskWrapper) {
                NamespaceContextSelector.pushCurrentSelector(((WeldTaskWrapper) r).currentSelector);
            }
        }

        @Override
        protected void afterExecute(Runnable r, Throwable t) {
            if (r instanceof WeldTaskWrapper) {
                NamespaceContextSelector.popCurrentSelector();
            }
        }

        @Override
        public void execute(Runnable command) {
            if (Container.instance().getState() == ContainerState.INITIALIZED) {
                WeldTaskWrapper task = new WeldTaskWrapper(command, NamespaceContextSelector.getCurrentSelector());
                super.execute(task);
            } else {
                super.execute(command);
            }
        }
    }

    static class WeldTaskWrapper implements Runnable {

        private final Runnable runnable;
        private final NamespaceContextSelector currentSelector;

        public WeldTaskWrapper(Runnable runnable, NamespaceContextSelector currentSelector) {
            this.runnable = runnable;
            this.currentSelector = currentSelector;
        }

        @Override
        public void run() {
            runnable.run();
        }
    }
}
