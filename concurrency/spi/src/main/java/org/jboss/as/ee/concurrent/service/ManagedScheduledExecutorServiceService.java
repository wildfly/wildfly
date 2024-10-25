/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jboss.as.ee.concurrent.service;

import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;
import java.util.function.Supplier;

import org.jboss.as.controller.ProcessStateNotifier;
import org.jboss.as.ee.concurrent.ConcurrencyImplementation;
import org.jboss.as.ee.concurrent.WildFlyManagedThreadFactory;
import org.jboss.as.ee.concurrent.WildFlyContextService;
import org.jboss.as.ee.concurrent.WildFlyManagedExecutorService;
import org.jboss.as.ee.concurrent.WildFlyManagedScheduledExecutorService;
import org.jboss.as.ee.concurrent.adapter.ManagedScheduledExecutorServiceAdapter;
import org.jboss.as.ee.logging.EeLogger;
import org.jboss.msc.service.StartContext;
import org.jboss.msc.service.StartException;
import org.jboss.msc.service.StopContext;
import org.wildfly.extension.requestcontroller.ControlPoint;
import org.wildfly.extension.requestcontroller.RequestController;

/**
 * Service responsible for creating, starting and stopping a ManagedScheduledExecutorServiceImpl.
 * <p/>
 * Note that the service's value is the executor's adapter, which does not allows lifecyle related invocations.
 *
 * @author Eduardo Martins
 */
public class ManagedScheduledExecutorServiceService extends EEConcurrentAbstractService<ManagedScheduledExecutorServiceAdapter> {

    private volatile ManagedScheduledExecutorServiceAdapter executorService;

    private final Consumer<ManagedScheduledExecutorServiceAdapter> consumer;
    private final String name;
    private final Supplier<WildFlyManagedThreadFactory> managedThreadFactorySupplier;
    private final long hungTaskThreshold;
    private final long hungTaskTerminationPeriod;
    private final boolean longRunningTasks;
    private final int corePoolSize;
    private final long keepAliveTime;
    private final TimeUnit keepAliveTimeUnit;
    private final long threadLifeTime;
    private final DelegatingSupplier<WildFlyContextService> contextServiceSupplier = new DelegatingSupplier<>();
    private final WildFlyManagedExecutorService.RejectPolicy rejectPolicy;
    private final Integer threadPriority;
    private final Supplier<ProcessStateNotifier> processStateNotifierSupplier;
    private final Supplier<RequestController> requestControllerSupplier;
    private ControlPoint controlPoint;
    private final Supplier<ManagedExecutorHungTasksPeriodicTerminationService> hungTasksPeriodicTerminationService;

    private Future hungTasksPeriodicTerminationFuture;

    /**
     * @param consumer
     * @param contextServiceSupplier
     * @param managedThreadFactorySupplier
     * @param processStateNotifierSupplier
     * @param requestControllerSupplier
     * @param name
     * @param jndiName
     * @param hungTaskThreshold
     * @param longRunningTasks
     * @param corePoolSize
     * @param keepAliveTime
     * @param keepAliveTimeUnit
     * @param threadLifeTime
     * @param rejectPolicy
     * @param threadPriority
     */
    public ManagedScheduledExecutorServiceService(final Consumer<ManagedScheduledExecutorServiceAdapter> consumer,
                                                  final Supplier<WildFlyContextService> contextServiceSupplier,
                                                  final Supplier<WildFlyManagedThreadFactory> managedThreadFactorySupplier,
                                                  final Supplier<ProcessStateNotifier> processStateNotifierSupplier,
                                                  final Supplier<RequestController> requestControllerSupplier,
                                                  String name, String jndiName, long hungTaskThreshold, long hungTaskTerminationPeriod, boolean longRunningTasks, int corePoolSize, long keepAliveTime, TimeUnit keepAliveTimeUnit, long threadLifeTime, WildFlyManagedExecutorService.RejectPolicy rejectPolicy, Integer threadPriority, final Supplier<ManagedExecutorHungTasksPeriodicTerminationService> hungTasksPeriodicTerminationService) {
        super(jndiName);
        this.consumer = consumer;
        this.contextServiceSupplier.set(contextServiceSupplier);
        this.managedThreadFactorySupplier = managedThreadFactorySupplier;
        this.processStateNotifierSupplier = processStateNotifierSupplier;
        this.requestControllerSupplier = requestControllerSupplier;
        this.name = name;
        this.hungTaskThreshold = hungTaskThreshold;
        this.hungTaskTerminationPeriod = hungTaskTerminationPeriod;
        this.longRunningTasks = longRunningTasks;
        this.corePoolSize = corePoolSize;
        this.keepAliveTime = keepAliveTime;
        this.keepAliveTimeUnit = keepAliveTimeUnit;
        this.threadLifeTime = threadLifeTime;
        this.rejectPolicy = rejectPolicy;
        this.threadPriority = threadPriority;
        this.hungTasksPeriodicTerminationService = hungTasksPeriodicTerminationService;
    }

    @Override
    void startValue(StartContext context) throws StartException {
        final int priority;
        if (threadPriority != null) {
            priority = threadPriority;
        } else {
            WildFlyManagedThreadFactory managedThreadFactory = managedThreadFactorySupplier != null ? managedThreadFactorySupplier.get() : null;
            priority = managedThreadFactory != null ? managedThreadFactory.getPriority() : Thread.NORM_PRIORITY;
        }
        WildFlyManagedThreadFactory managedThreadFactory = ConcurrencyImplementation.INSTANCE.newManagedThreadFactory("EE-ManagedScheduledExecutorService-" + name, null, priority);
        if (requestControllerSupplier != null) {
            final RequestController requestController = requestControllerSupplier.get();
            controlPoint = requestController != null ? requestController.getControlPoint(name, "managed-scheduled-executor-service") : null;
        }
        executorService = new ManagedScheduledExecutorServiceAdapter(ConcurrencyImplementation.INSTANCE.newManagedScheduledExecutorService(name, managedThreadFactory, hungTaskThreshold, longRunningTasks, corePoolSize, keepAliveTime, keepAliveTimeUnit, threadLifeTime, contextServiceSupplier != null ? contextServiceSupplier.get() : null, rejectPolicy, controlPoint, processStateNotifierSupplier.get()));
        if (hungTaskThreshold > 0 && hungTaskTerminationPeriod > 0) {
            hungTasksPeriodicTerminationFuture = hungTasksPeriodicTerminationService.get().startHungTaskPeriodicTermination(executorService, hungTaskTerminationPeriod);
        }
        consumer.accept(executorService);
    }

    @Override
    void stopValue(StopContext context) {
        consumer.accept(null);
        if (executorService != null) {
            if (hungTasksPeriodicTerminationFuture != null) {
                hungTasksPeriodicTerminationFuture.cancel(true);
            }
            executorService.getExecutorService().shutdownNow();
            executorService.getWildFlyManagedThreadFactory().stop();
            this.executorService = null;
        }
        if(controlPoint != null) {
            requestControllerSupplier.get().removeControlPoint(controlPoint);
        }
    }

    public ManagedScheduledExecutorServiceAdapter getValue() throws IllegalStateException {
        return executorService;
    }

    public WildFlyManagedScheduledExecutorService getExecutorService() throws IllegalStateException {
        if (executorService == null) {
            throw EeLogger.ROOT_LOGGER.concurrentServiceValueUninitialized();
        }
        return executorService.getExecutorService();
    }

    public DelegatingSupplier<WildFlyContextService> getContextServiceSupplier() {
        return contextServiceSupplier;
    }

}
