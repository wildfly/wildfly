/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2020 Red Hat, Inc., and individual contributors
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
package org.jboss.as.ee.concurrent.service;

import static org.wildfly.common.Assert.checkNotNullParamWithNullPointerException;

import org.jboss.as.controller.OperationContext;
import org.jboss.as.ee.concurrent.ManagedExecutorWithHungThreads;
import org.jboss.msc.Service;
import org.jboss.msc.service.ServiceBuilder;
import org.jboss.msc.service.StartContext;
import org.jboss.msc.service.StartException;
import org.jboss.msc.service.StopContext;

import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;

/**
 * Periodic hung task termination service for managed executors.
 * @author emmartins
 */
public class ManagedExecutorHungTasksPeriodicTerminationService implements Service {

    private volatile ScheduledExecutorService scheduler;
    private Consumer<ManagedExecutorHungTasksPeriodicTerminationService> consumer;

    public void install(OperationContext context) {
        ServiceBuilder serviceBuilder = context.getServiceTarget()
                .addService(ConcurrentServiceNames.HUNG_TASK_PERIODIC_TERMINATION_SERVICE_NAME)
                .setInstance(this);
        consumer = serviceBuilder.provides(ConcurrentServiceNames.HUNG_TASK_PERIODIC_TERMINATION_SERVICE_NAME);
        serviceBuilder.install();
    }

    @Override
    public void start(StartContext startContext) throws StartException {
        consumer.accept(this);
    }

    @Override
    public void stop(StopContext stopContext) {
        if (scheduler != null) {
            scheduler.shutdownNow();
            scheduler = null;
        }
        consumer.accept(null);
    }

    /**
     * Starts the periodic hang task termination for the specified executor.
     * @param executor
     * @param hungTaskTerminationPeriod
     * @return a Future instance which may be used to cancel the hung task periodic termination
     */
    public synchronized Future startHungTaskPeriodicTermination(final ManagedExecutorWithHungThreads executor, final long hungTaskTerminationPeriod) {
        checkNotNullParamWithNullPointerException("executor", executor);
        if (hungTaskTerminationPeriod <= 0) {
            throw new IllegalArgumentException("hungTaskTerminationPeriod is not > 0");
        }
        if (scheduler == null) {
            scheduler = Executors.newSingleThreadScheduledExecutor(new ThreadFactory());
        }
        return scheduler.scheduleAtFixedRate(() -> executor.terminateHungTasks(), hungTaskTerminationPeriod, hungTaskTerminationPeriod, TimeUnit.MILLISECONDS);
    }

    /**
     * Wrapper of default threadfactory, just to override thread names.
     */
    private static class ThreadFactory implements java.util.concurrent.ThreadFactory {
        private final java.util.concurrent.ThreadFactory threadFactory = Executors.defaultThreadFactory();
        /**
         *
         * @param r
         * @return
         */
        @Override
        public Thread newThread(Runnable r) {
            final Thread t = threadFactory.newThread(r);
            t.setName("managed-executor-hungtaskperiodictermination-"+t.getName());
            return t;
        }
    }
}
