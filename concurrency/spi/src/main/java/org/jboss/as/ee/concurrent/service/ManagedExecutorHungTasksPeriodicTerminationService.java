/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.jboss.as.ee.concurrent.service;

import static org.wildfly.common.Assert.checkNotNullParamWithNullPointerException;

import org.jboss.as.controller.OperationContext;
import org.jboss.as.ee.concurrent.WildFlyManagedExecutorService;
import org.jboss.as.ee.logging.EeLogger;
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
    public synchronized Future startHungTaskPeriodicTermination(final WildFlyManagedExecutorService executor, final long hungTaskTerminationPeriod) {
        checkNotNullParamWithNullPointerException("executor", executor);
        if (hungTaskTerminationPeriod <= 0) {
            throw EeLogger.ROOT_LOGGER.hungTaskTerminationPeriodIsNotBiggerThanZero();
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
