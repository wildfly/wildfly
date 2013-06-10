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
import org.glassfish.enterprise.concurrent.ManagedExecutorServiceImpl;
import org.glassfish.enterprise.concurrent.ManagedThreadFactoryImpl;
import org.jboss.as.ee.EeMessages;
import org.jboss.msc.inject.Injector;
import org.jboss.msc.service.Service;
import org.jboss.msc.service.StartContext;
import org.jboss.msc.service.StartException;
import org.jboss.msc.service.StopContext;
import org.jboss.msc.value.InjectedValue;

import java.util.concurrent.TimeUnit;

/**
 * Service responsible for creating, starting and stopping a ManagedExecutorServiceImpl.
 * <p/>
 * Note that the service's value is the executor's adapter, which does not allows lifecyle related invocations.
 *
 * @author Eduardo Martins
 */
public class ManagedExecutorServiceService implements Service<ManagedExecutorServiceAdapter> {

    public static final Class<?> SERVICE_VALUE_TYPE = ManagedExecutorServiceAdapter.class;

    private ManagedExecutorServiceImpl executorService;

    private final String name;
    private final InjectedValue<ManagedThreadFactoryImpl> managedThreadFactory;
    private final long hungTaskThreshold;
    private final boolean longRunningTasks;
    private final int corePoolSize;
    private final int maxPoolSize;
    private final long keepAliveTime;
    private final TimeUnit keepAliveTimeUnit;
    private final long threadLifeTime;
    private final int queueCapacity;
    private final InjectedValue<ContextServiceImpl> contextService = new InjectedValue<>();
    private final AbstractManagedExecutorService.RejectPolicy rejectPolicy;

    /**
     * @param name
     * @param hungTaskThreshold
     * @param longRunningTasks
     * @param corePoolSize
     * @param maxPoolSize
     * @param keepAliveTime
     * @param keepAliveTimeUnit
     * @param threadLifeTime
     * @param queueCapacity
     * @param rejectPolicy
     * @see ManagedExecutorServiceImpl#ManagedExecutorServiceImpl(String, org.glassfish.enterprise.concurrent.ManagedThreadFactoryImpl, long, boolean, int, int, long, java.util.concurrent.TimeUnit, long, int, org.glassfish.enterprise.concurrent.ContextServiceImpl, org.glassfish.enterprise.concurrent.AbstractManagedExecutorService.RejectPolicy)
     */
    public ManagedExecutorServiceService(String name, long hungTaskThreshold, boolean longRunningTasks, int corePoolSize, int maxPoolSize, long keepAliveTime, TimeUnit keepAliveTimeUnit, long threadLifeTime, int queueCapacity, AbstractManagedExecutorService.RejectPolicy rejectPolicy) {
        this.name = name;
        this.managedThreadFactory = new InjectedValue<>();
        this.hungTaskThreshold = hungTaskThreshold;
        this.longRunningTasks = longRunningTasks;
        this.corePoolSize = corePoolSize;
        this.maxPoolSize = maxPoolSize;
        this.keepAliveTime = keepAliveTime;
        this.keepAliveTimeUnit = keepAliveTimeUnit;
        this.threadLifeTime = threadLifeTime;
        this.queueCapacity = queueCapacity;
        this.rejectPolicy = rejectPolicy;
    }

    public void start(final StartContext context) throws StartException {
        executorService = new ManagedExecutorServiceImpl(name, managedThreadFactory.getValue(), hungTaskThreshold, longRunningTasks, corePoolSize, maxPoolSize, keepAliveTime, keepAliveTimeUnit, threadLifeTime, queueCapacity, contextService.getOptionalValue(), rejectPolicy);
    }

    public void stop(final StopContext context) {
        if (executorService != null) {
            executorService.shutdownNow();
            this.executorService = null;
        }
    }

    public ManagedExecutorServiceAdapter getValue() throws IllegalStateException {
        if (executorService == null) {
            throw EeMessages.MESSAGES.concurrentServiceValueUninitialized();
        }
        return executorService.getAdapter();
    }

    public Injector<ManagedThreadFactoryImpl> getManagedThreadFactoryInjector() {
        return managedThreadFactory;
    }

    public Injector<ContextServiceImpl> getContextServiceInjector() {
        return contextService;
    }

}
