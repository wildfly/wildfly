/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.jboss.as.ee.concurrent;

import org.jboss.as.controller.ProcessStateNotifier;
import org.wildfly.extension.requestcontroller.ControlPoint;

import java.util.concurrent.BlockingQueue;
import java.util.concurrent.TimeUnit;

/**
 * ConcurrencyImplementation for Concurro 3.0
 */
public class ConcurrencyImplementation30 extends AbstractConcurrencyImplementation {

    @Override
    public String getJBossModuleName() {
        return "org.glassfish.jakarta.enterprise.concurrent";
    }

    @Override
    public WildFlyContextService newContextService(String name, ContextServiceTypesConfiguration contextServiceTypesConfiguration) {
        return new ContextServiceImpl(name, contextServiceTypesConfiguration);
    }

    @Override
    public WildFlyManagedThreadFactory newManagedThreadFactory(String name, WildFlyContextService contextService, int priority) {
        return new ManagedThreadFactoryImpl(name, contextService, priority);
    }

    @Override
    public WildFlyManagedExecutorService newManagedExecutorService(String name, WildFlyManagedThreadFactory managedThreadFactory, long hungTaskThreshold, boolean longRunningTasks, int corePoolSize, int maxPoolSize, long keepAliveTime, TimeUnit keepAliveTimeUnit, long threadLifeTime, WildFlyContextService contextService, WildFlyManagedExecutorService.RejectPolicy rejectPolicy, BlockingQueue<Runnable> queue, ControlPoint controlPoint, ProcessStateNotifier processStateNotifier) {
        return new ManagedExecutorServiceImpl(name, managedThreadFactory, hungTaskThreshold, longRunningTasks, corePoolSize, maxPoolSize, keepAliveTime, keepAliveTimeUnit, threadLifeTime, contextService, rejectPolicy, queue, controlPoint, processStateNotifier);
    }

    @Override
    public WildFlyManagedExecutorService newManagedExecutorService(String name, WildFlyManagedThreadFactory managedThreadFactory, long hungTaskThreshold, boolean longRunningTasks, int corePoolSize, int maxPoolSize, long keepAliveTime, TimeUnit keepAliveTimeUnit, long threadLifeTime, int queueCapacity, WildFlyContextService contextService, WildFlyManagedExecutorService.RejectPolicy rejectPolicy, ControlPoint controlPoint, ProcessStateNotifier processStateNotifier) {
        return new ManagedExecutorServiceImpl(name, managedThreadFactory, hungTaskThreshold, longRunningTasks, corePoolSize, maxPoolSize, keepAliveTime, keepAliveTimeUnit, threadLifeTime, queueCapacity, contextService, rejectPolicy, controlPoint, processStateNotifier);
    }

    @Override
    public WildFlyManagedScheduledExecutorService newManagedScheduledExecutorService(String name, WildFlyManagedThreadFactory managedThreadFactory, long hungTaskThreshold, boolean longRunningTasks, int corePoolSize, long keepAliveTime, TimeUnit keepAliveTimeUnit, long threadLifeTime, WildFlyContextService contextService, WildFlyManagedExecutorService.RejectPolicy rejectPolicy, ControlPoint controlPoint, ProcessStateNotifier processStateNotifier) {
        return new ManagedScheduledExecutorServiceImpl(name, managedThreadFactory, hungTaskThreshold, longRunningTasks, corePoolSize, keepAliveTime, keepAliveTimeUnit, threadLifeTime, contextService, rejectPolicy, controlPoint, processStateNotifier);
    }

    @Override
    public String toString() {
        return "Concurro 3.0";
    }
}
