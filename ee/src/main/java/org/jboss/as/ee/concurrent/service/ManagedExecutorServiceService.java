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

package org.jboss.as.ee.concurrent.service;

import java.util.function.Consumer;
import java.util.function.Supplier;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import org.glassfish.enterprise.concurrent.AbstractManagedExecutorService;
import org.glassfish.enterprise.concurrent.ContextServiceImpl;
import org.glassfish.enterprise.concurrent.ManagedExecutorServiceAdapter;
import org.jboss.as.ee.concurrent.ManagedExecutorServiceImpl;
import org.jboss.as.ee.concurrent.ManagedThreadFactoryImpl;
import org.jboss.as.ee.logging.EeLogger;
import org.jboss.msc.service.StartContext;
import org.jboss.msc.service.StartException;
import org.jboss.msc.service.StopContext;
import org.wildfly.extension.requestcontroller.ControlPoint;
import org.wildfly.extension.requestcontroller.RequestController;

/**
 * Service responsible for creating, starting and stopping a ManagedExecutorServiceImpl.
 * <p/>
 * Note that the service's value is the executor's adapter, which does not allows lifecyle related invocations.
 *
 * @author Eduardo Martins
 */
public class ManagedExecutorServiceService extends EEConcurrentAbstractService<ManagedExecutorServiceAdapter> {

    private volatile ManagedExecutorServiceImpl executorService;

    private final Consumer<ManagedExecutorServiceAdapter> consumer;

    private final String name;
    private final Supplier<ManagedThreadFactoryImpl> managedThreadFactorySupplier;
    private final long hungTaskThreshold;
    private final long hungTaskTerminationPeriod;
    private final boolean longRunningTasks;
    private final int corePoolSize;
    private final int maxPoolSize;
    private final long keepAliveTime;
    private final TimeUnit keepAliveTimeUnit;
    private final long threadLifeTime;
    private final int queueCapacity;
    private final DelegatingSupplier<ContextServiceImpl> contextServiceSupplier = new DelegatingSupplier<>();
    private final AbstractManagedExecutorService.RejectPolicy rejectPolicy;
    private final Integer threadPriority;
    private final Supplier<RequestController> requestControllerSupplier;
    private ControlPoint controlPoint;
    private final Supplier<ManagedExecutorHungTasksPeriodicTerminationService> hungTasksPeriodicTerminationService;

    private Future hungTasksPeriodicTerminationFuture;

    /**
     * @param consumer
     * @param contextServiceSupplier
     * @param managedThreadFactorySupplier
     * @param requestControllerSupplier
     * @param name
     * @param jndiName
     * @param hungTaskThreshold
     * @param hungTaskTerminationPeriod
     * @param longRunningTasks
     * @param corePoolSize
     * @param maxPoolSize
     * @param keepAliveTime
     * @param keepAliveTimeUnit
     * @param threadLifeTime
     * @param queueCapacity
     * @param rejectPolicy
     * @param threadPriority
     * @see ManagedExecutorServiceImpl#ManagedExecutorServiceImpl(String, org.jboss.as.ee.concurrent.ManagedThreadFactoryImpl, long, boolean, int, int, long, java.util.concurrent.TimeUnit, long, int, org.glassfish.enterprise.concurrent.ContextServiceImpl, org.glassfish.enterprise.concurrent.AbstractManagedExecutorService.RejectPolicy, org.wildfly.extension.requestcontroller.ControlPoint)
     */
    public ManagedExecutorServiceService(final Consumer<ManagedExecutorServiceAdapter> consumer,
                                         final Supplier<ContextServiceImpl> contextServiceSupplier,
                                         final Supplier<ManagedThreadFactoryImpl> managedThreadFactorySupplier,
                                         final Supplier<RequestController> requestControllerSupplier,
                                         String name, String jndiName, long hungTaskThreshold, long hungTaskTerminationPeriod, boolean longRunningTasks, int corePoolSize, int maxPoolSize, long keepAliveTime, TimeUnit keepAliveTimeUnit, long threadLifeTime, int queueCapacity, AbstractManagedExecutorService.RejectPolicy rejectPolicy, Integer threadPriority, final Supplier<ManagedExecutorHungTasksPeriodicTerminationService> hungTasksPeriodicTerminationService) {
        super(jndiName);
        this.consumer = consumer;
        this.contextServiceSupplier.set(contextServiceSupplier);
        this.managedThreadFactorySupplier = managedThreadFactorySupplier;
        this.requestControllerSupplier = requestControllerSupplier;
        this.name = name;
        this.hungTaskThreshold = hungTaskThreshold;
        this.hungTaskTerminationPeriod = hungTaskTerminationPeriod;
        this.longRunningTasks = longRunningTasks;
        this.corePoolSize = corePoolSize;
        this.maxPoolSize = maxPoolSize;
        this.keepAliveTime = keepAliveTime;
        this.keepAliveTimeUnit = keepAliveTimeUnit;
        this.threadLifeTime = threadLifeTime;
        this.queueCapacity = queueCapacity;
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
            ManagedThreadFactoryImpl managedThreadFactory = managedThreadFactorySupplier != null ? managedThreadFactorySupplier.get() : null;
            priority = managedThreadFactory != null ? managedThreadFactory.getPriority() : Thread.NORM_PRIORITY;
        }
        ManagedThreadFactoryImpl managedThreadFactory = new ManagedThreadFactoryImpl("EE-ManagedExecutorService-"+name, null, priority);
        if (requestControllerSupplier != null) {
            final RequestController requestController = requestControllerSupplier.get();
            controlPoint = requestController != null ? requestController.getControlPoint(name, "managed-executor-service") : null;
        }
        executorService = new ManagedExecutorServiceImpl(name, managedThreadFactory, hungTaskThreshold, longRunningTasks, corePoolSize, maxPoolSize, keepAliveTime, keepAliveTimeUnit, threadLifeTime, queueCapacity, contextServiceSupplier != null ? contextServiceSupplier.get() : null, rejectPolicy, controlPoint);
        if (hungTaskThreshold > 0 && hungTaskTerminationPeriod > 0) {
            hungTasksPeriodicTerminationFuture = hungTasksPeriodicTerminationService.get().startHungTaskPeriodicTermination(executorService, hungTaskTerminationPeriod);
        }
        consumer.accept(executorService.getAdapter());
    }

    @Override
    void stopValue(StopContext context) {
        if (executorService != null) {
            if (hungTasksPeriodicTerminationFuture != null) {
                hungTasksPeriodicTerminationFuture.cancel(true);
            }
            executorService.shutdownNow();
            executorService.getManagedThreadFactory().stop();
            this.executorService = null;
        }
        if(controlPoint != null) {
            requestControllerSupplier.get().removeControlPoint(controlPoint);
        }
        consumer.accept(null);
    }

    public ManagedExecutorServiceAdapter getValue() throws IllegalStateException {
        return getExecutorService().getAdapter();
    }

    public ManagedExecutorServiceImpl getExecutorService() throws IllegalStateException {
        if (executorService == null) {
            throw EeLogger.ROOT_LOGGER.concurrentServiceValueUninitialized();
        }
        return executorService;
    }

    public DelegatingSupplier<ContextServiceImpl> getContextServiceSupplier() {
        return contextServiceSupplier;
    }

}
