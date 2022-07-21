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

import org.glassfish.enterprise.concurrent.AbstractManagedExecutorService;
import org.glassfish.enterprise.concurrent.ContextServiceImpl;
import org.glassfish.enterprise.concurrent.ManagedScheduledExecutorServiceAdapter;
import org.jboss.as.ee.concurrent.ManagedScheduledExecutorServiceImpl;
import org.jboss.as.ee.concurrent.ManagedThreadFactoryImpl;
import org.jboss.as.ee.logging.EeLogger;
import org.jboss.msc.inject.Injector;
import org.jboss.msc.service.StartContext;
import org.jboss.msc.service.StartException;
import org.jboss.msc.service.StopContext;
import org.jboss.msc.value.InjectedValue;
import org.wildfly.extension.requestcontroller.ControlPoint;
import org.wildfly.extension.requestcontroller.RequestController;

import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;

/**
 * Service responsible for creating, starting and stopping a ManagedScheduledExecutorServiceImpl.
 * <p/>
 * Note that the service's value is the executor's adapter, which does not allows lifecyle related invocations.
 *
 * @author Eduardo Martins
 */
public class ManagedScheduledExecutorServiceService extends EEConcurrentAbstractService<ManagedScheduledExecutorServiceAdapter> {

    private volatile ManagedScheduledExecutorServiceImpl executorService;

    private final String name;
    private final InjectedValue<ManagedThreadFactoryImpl> managedThreadFactoryInjectedValue;
    private final long hungTaskThreshold;
    private final long hungTaskTerminationPeriod;
    private final boolean longRunningTasks;
    private final int corePoolSize;
    private final long keepAliveTime;
    private final TimeUnit keepAliveTimeUnit;
    private final long threadLifeTime;
    private final InjectedValue<ContextServiceImpl> contextService;
    private final AbstractManagedExecutorService.RejectPolicy rejectPolicy;
    private final Integer threadPriority;
    private final InjectedValue<RequestController> requestController = new InjectedValue<>();
    private ControlPoint controlPoint;
    private final Supplier<ManagedExecutorHungTasksPeriodicTerminationService> hungTasksPeriodicTerminationService;

    private Future hungTasksPeriodicTerminationFuture;

    /**
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
     * @see ManagedScheduledExecutorServiceImpl#ManagedScheduledExecutorServiceImpl(String, org.jboss.as.ee.concurrent.ManagedThreadFactoryImpl, long, boolean, int, long, java.util.concurrent.TimeUnit, long, org.glassfish.enterprise.concurrent.ContextServiceImpl, org.glassfish.enterprise.concurrent.AbstractManagedExecutorService.RejectPolicy, org.wildfly.extension.requestcontroller.ControlPoint)
     */
    public ManagedScheduledExecutorServiceService(String name, String jndiName, long hungTaskThreshold, long hungTaskTerminationPeriod, boolean longRunningTasks, int corePoolSize, long keepAliveTime, TimeUnit keepAliveTimeUnit, long threadLifeTime, AbstractManagedExecutorService.RejectPolicy rejectPolicy, Integer threadPriority, final Supplier<ManagedExecutorHungTasksPeriodicTerminationService> hungTasksPeriodicTerminationService) {
        super(jndiName);
        this.name = name;
        this.managedThreadFactoryInjectedValue = new InjectedValue<>();
        this.hungTaskThreshold = hungTaskThreshold;
        this.hungTaskTerminationPeriod = hungTaskTerminationPeriod;
        this.longRunningTasks = longRunningTasks;
        this.corePoolSize = corePoolSize;
        this.keepAliveTime = keepAliveTime;
        this.keepAliveTimeUnit = keepAliveTimeUnit;
        this.threadLifeTime = threadLifeTime;
        this.contextService = new InjectedValue<>();
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
            ManagedThreadFactoryImpl managedThreadFactory = managedThreadFactoryInjectedValue.getOptionalValue();
            priority = managedThreadFactory != null ? managedThreadFactory.getPriority() : Thread.NORM_PRIORITY;
        }
        ManagedThreadFactoryImpl managedThreadFactory = new ManagedThreadFactoryImpl("EE-ManagedScheduledExecutorService-" + name, null, priority);
        if(requestController.getOptionalValue() != null) {
            controlPoint = requestController.getValue().getControlPoint(name, "managed-scheduled-executor-service");
        }
        executorService = new ManagedScheduledExecutorServiceImpl(name, managedThreadFactory, hungTaskThreshold, longRunningTasks, corePoolSize, keepAliveTime, keepAliveTimeUnit, threadLifeTime, contextService.getOptionalValue(), rejectPolicy, controlPoint);
        if (hungTaskThreshold > 0 && hungTaskTerminationPeriod > 0) {
            hungTasksPeriodicTerminationFuture = hungTasksPeriodicTerminationService.get().startHungTaskPeriodicTermination(executorService, hungTaskTerminationPeriod);
        }
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
            requestController.getValue().removeControlPoint(controlPoint);
        }
    }

    public ManagedScheduledExecutorServiceAdapter getValue() throws IllegalStateException {
        return getExecutorService().getAdapter();
    }

    public ManagedScheduledExecutorServiceImpl getExecutorService() throws IllegalStateException {
        if (executorService == null) {
            throw EeLogger.ROOT_LOGGER.concurrentServiceValueUninitialized();
        }
        return executorService;
    }

    public Injector<ManagedThreadFactoryImpl> getManagedThreadFactoryInjector() {
        return managedThreadFactoryInjectedValue;
    }

    public Injector<ContextServiceImpl> getContextServiceInjector() {
        return contextService;
    }

    public InjectedValue<RequestController> getRequestController() {
        return requestController;
    }
}
