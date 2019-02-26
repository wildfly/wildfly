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
import org.glassfish.enterprise.concurrent.ManagedExecutorServiceAdapter;
import org.glassfish.enterprise.concurrent.ManagedThreadFactoryImpl;
import org.jboss.as.ee.concurrent.ManagedExecutorServiceImpl;
import org.jboss.as.ee.logging.EeLogger;
import org.jboss.msc.service.StartContext;
import org.jboss.msc.service.StopContext;
import org.wildfly.extension.requestcontroller.ControlPoint;
import org.wildfly.extension.requestcontroller.RequestController;

import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;

/**
 * Service responsible for creating, starting and stopping a ManagedExecutorServiceImpl.
 * <p/>
 * Note that the service's value is the executor's adapter, which does not allows lifecyle related invocations.
 *
 * @author Eduardo Martins
 * @author <a href="mailto:ropalka@redhat.com">Richard Opalka</a>
 */
public class ManagedExecutorServiceService extends EEConcurrentAbstractService<ManagedExecutorServiceAdapter> {

    private volatile ManagedExecutorServiceImpl executorService;

    private final String name;
    private final long hungTaskThreshold;
    private final boolean longRunningTasks;
    private final int corePoolSize;
    private final int maxPoolSize;
    private final long keepAliveTime;
    private final TimeUnit keepAliveTimeUnit;
    private final long threadLifeTime;
    private final int queueCapacity;
    private final AbstractManagedExecutorService.RejectPolicy rejectPolicy;
    private final Supplier<ContextServiceImpl> contextServiceSupplier;
    private final Supplier<ManagedThreadFactoryImpl> managedThreadFactorySupplier;
    private final Supplier<RequestController> requestControllerSupplier;
    private ControlPoint controlPoint;

    /**
     * @param name
     * @param jndiName
     * @param hungTaskThreshold
     * @param longRunningTasks
     * @param corePoolSize
     * @param maxPoolSize
     * @param keepAliveTime
     * @param keepAliveTimeUnit
     * @param threadLifeTime
     * @param queueCapacity
     * @param rejectPolicy
     * @param contextServiceSupplier
     * @param managedThreadFactorySupplier
     * @param requestControllerSupplier
     * @see ManagedExecutorServiceImpl#ManagedExecutorServiceImpl(String, org.glassfish.enterprise.concurrent.ManagedThreadFactoryImpl, long, boolean, int, int, long, java.util.concurrent.TimeUnit, long, int, org.glassfish.enterprise.concurrent.ContextServiceImpl, org.glassfish.enterprise.concurrent.AbstractManagedExecutorService.RejectPolicy, org.wildfly.extension.requestcontroller.ControlPoint)
     */
    public ManagedExecutorServiceService(String name, String jndiName, long hungTaskThreshold, boolean longRunningTasks, int corePoolSize, int maxPoolSize, long keepAliveTime, TimeUnit keepAliveTimeUnit, long threadLifeTime, int queueCapacity, AbstractManagedExecutorService.RejectPolicy rejectPolicy,
                                         final Supplier<ContextServiceImpl> contextServiceSupplier,
                                         final Supplier<ManagedThreadFactoryImpl> managedThreadFactorySupplier,
                                         final Supplier<RequestController> requestControllerSupplier) {
        super(jndiName);
        this.name = name;
        this.hungTaskThreshold = hungTaskThreshold;
        this.longRunningTasks = longRunningTasks;
        this.corePoolSize = corePoolSize;
        this.maxPoolSize = maxPoolSize;
        this.keepAliveTime = keepAliveTime;
        this.keepAliveTimeUnit = keepAliveTimeUnit;
        this.threadLifeTime = threadLifeTime;
        this.queueCapacity = queueCapacity;
        this.rejectPolicy = rejectPolicy;
        this.contextServiceSupplier = contextServiceSupplier;
        this.managedThreadFactorySupplier = managedThreadFactorySupplier;
        this.requestControllerSupplier = requestControllerSupplier;
    }

    @Override
    void startValue(final StartContext context) {
        ManagedThreadFactoryImpl managedThreadFactory = managedThreadFactorySupplier != null ? managedThreadFactorySupplier.get() : null;
        if(managedThreadFactory == null) {
            // if not injected create one using normal thread priority
            final String threadFactoryName = "EE-ManagedExecutorService-"+name;
            managedThreadFactory = new ElytronManagedThreadFactory(threadFactoryName, null, Thread.NORM_PRIORITY);
        }

        if (requestControllerSupplier != null) {
            controlPoint = requestControllerSupplier.get().getControlPoint(name, "managed-executor-service");
        }
        ContextServiceImpl ctxService = contextServiceSupplier != null ? contextServiceSupplier.get() : null;
        executorService = new ManagedExecutorServiceImpl(name, managedThreadFactory, hungTaskThreshold, longRunningTasks, corePoolSize, maxPoolSize, keepAliveTime, keepAliveTimeUnit, threadLifeTime, queueCapacity, ctxService, rejectPolicy, controlPoint);

    }

    @Override
    void stopValue(final StopContext context) {
        if (executorService != null) {
            executorService.shutdownNow();
            if (managedThreadFactorySupplier == null) {
                // if not injected the thread factory was created on start, and now needs to stop
                executorService.getManagedThreadFactory().stop();
            }
            this.executorService = null;
        }
        if(controlPoint != null) {
            requestControllerSupplier.get().removeControlPoint(controlPoint);
        }
    }

    public ManagedExecutorServiceAdapter getValue() throws IllegalStateException {
        if (executorService == null) {
            throw EeLogger.ROOT_LOGGER.concurrentServiceValueUninitialized();
        }
        return executorService.getAdapter();
    }

}
