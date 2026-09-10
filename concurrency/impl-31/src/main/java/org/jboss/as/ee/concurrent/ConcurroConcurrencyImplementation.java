/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.jboss.as.ee.concurrent;

import jakarta.enterprise.concurrent.ManagedExecutorService;
import jakarta.enterprise.concurrent.ManagedScheduledExecutorService;
import jakarta.enterprise.concurrent.ManagedThreadFactory;
import org.jboss.as.controller.ProcessStateNotifier;
import org.jboss.as.ee.concurrent.deployers.ConcurrencyManagedCDIBeansBindingProcessor;
import org.jboss.as.ee.concurrent.deployers.ConcurrencyManagedCDIBeansDescriptorProcessor;
import org.jboss.as.ee.logging.EeLogger;
import org.jboss.as.ee.subsystem.EeExtension;
import org.jboss.as.server.DeploymentProcessorTarget;
import org.jboss.as.server.deployment.Phase;
import org.wildfly.extension.requestcontroller.ControlPoint;

import java.util.concurrent.BlockingQueue;
import java.util.concurrent.TimeUnit;

/**
 * ConcurrencyImplementation for Concurro 3.1
 */
public class ConcurroConcurrencyImplementation extends AbstractConcurrencyImplementation {

    @Override
    public void addDeploymentProcessors(DeploymentProcessorTarget processorTarget) {
        super.addDeploymentProcessors(processorTarget);
        processorTarget.addDeploymentProcessor(EeExtension.SUBSYSTEM_NAME, Phase.POST_MODULE, Phase.POST_MODULE_CONCURRO_CDI_BEANS_BINDING, new ConcurrencyManagedCDIBeansBindingProcessor());
        processorTarget.addDeploymentProcessor(EeExtension.SUBSYSTEM_NAME, Phase.POST_MODULE, Phase.POST_MODULE_CONCURRO_CDI_BEANS_DESCRIPTOR, new ConcurrencyManagedCDIBeansDescriptorProcessor());
    }

    @Override
    public String getJBossModuleName() {
        return "org.glassfish.concurro";
    }

    @Override
    public WildFlyContextService newContextService(String name, ContextServiceTypesConfiguration contextServiceTypesConfiguration) {
        return new ConcurroContextServiceImpl(name, contextServiceTypesConfiguration);
    }

    @Override
    public WildFlyManagedThreadFactory newManagedThreadFactory(String name, WildFlyContextService contextService, int priority, boolean virtual) {
        WildFlyManagedThreadFactory threadFactory = null;
        if (virtual) {
            try {
                // thread priority is ignored, virtual threads always use Thread.NORM_PRIORITY
                threadFactory = new ConcurroVirtualThreadsManagedThreadFactoryImpl(name, contextService);
            } catch (Exception e) {
                EeLogger.ROOT_LOGGER.failedToCreateVirtualThreadsResource(ManagedThreadFactory.class, e);
            }
        }
        if (threadFactory == null) {
            threadFactory = new ConcurroManagedThreadFactoryImpl(name, contextService, priority);        }
        return threadFactory;
    }

    @Override
    public WildFlyManagedExecutorService newManagedExecutorService(String name, WildFlyManagedThreadFactory managedThreadFactory, long hungTaskThreshold, boolean longRunningTasks, int corePoolSize, int maxPoolSize, long keepAliveTime, TimeUnit keepAliveTimeUnit, long threadLifeTime, WildFlyContextService contextService, WildFlyManagedExecutorService.RejectPolicy rejectPolicy, BlockingQueue<Runnable> queue, ControlPoint controlPoint, ProcessStateNotifier processStateNotifier, boolean virtual) {
        return newManagedExecutorService(name, managedThreadFactory, hungTaskThreshold, longRunningTasks, corePoolSize, maxPoolSize, keepAliveTime, keepAliveTimeUnit, threadLifeTime, 0, contextService, rejectPolicy, controlPoint, processStateNotifier, virtual);
    }

    @Override
    public WildFlyManagedExecutorService newManagedExecutorService(String name, WildFlyManagedThreadFactory managedThreadFactory, long hungTaskThreshold, boolean longRunningTasks, int corePoolSize, int maxPoolSize, long keepAliveTime, TimeUnit keepAliveTimeUnit, long threadLifeTime, int queueCapacity, WildFlyContextService contextService, WildFlyManagedExecutorService.RejectPolicy rejectPolicy, ControlPoint controlPoint, ProcessStateNotifier processStateNotifier, boolean virtual) {
        WildFlyManagedExecutorService managedExecutorService = null;
        if (virtual) {
            try {
                managedExecutorService = new ConcurroVirtualThreadsManagedExecutorServiceImpl(name, managedThreadFactory, hungTaskThreshold, longRunningTasks, maxPoolSize, queueCapacity, contextService, rejectPolicy, controlPoint, processStateNotifier);
            } catch (Exception e) {
                EeLogger.ROOT_LOGGER.failedToCreateVirtualThreadsResource(ManagedExecutorService.class, e);
            }
        }
        if (managedExecutorService == null) {
            managedExecutorService = new ConcurroManagedExecutorServiceImpl(name, managedThreadFactory, hungTaskThreshold, longRunningTasks, corePoolSize, maxPoolSize, keepAliveTime, keepAliveTimeUnit, threadLifeTime, queueCapacity, contextService, rejectPolicy, controlPoint, processStateNotifier);
        }
        return managedExecutorService;
    }

    @Override
    public WildFlyManagedScheduledExecutorService newManagedScheduledExecutorService(String name, WildFlyManagedThreadFactory managedThreadFactory, long hungTaskThreshold, boolean longRunningTasks, int corePoolSize, long keepAliveTime, TimeUnit keepAliveTimeUnit, long threadLifeTime, WildFlyContextService contextService, WildFlyManagedExecutorService.RejectPolicy rejectPolicy, ControlPoint controlPoint, ProcessStateNotifier processStateNotifier, boolean virtual) {
        WildFlyManagedScheduledExecutorService managedExecutorService = null;
        if (virtual) {
            try {
                managedExecutorService = new ConcurroVirtualThreadsManagedScheduledExecutorServiceImpl(name, managedThreadFactory, hungTaskThreshold, longRunningTasks, corePoolSize, keepAliveTime, keepAliveTimeUnit, threadLifeTime, contextService, rejectPolicy, controlPoint, processStateNotifier);
            } catch (Exception e) {
                EeLogger.ROOT_LOGGER.failedToCreateVirtualThreadsResource(ManagedScheduledExecutorService.class, e);
            }
        }
        if (managedExecutorService == null) {
            managedExecutorService = new ConcurroManagedScheduledExecutorServiceImpl(name, managedThreadFactory, hungTaskThreshold, longRunningTasks, corePoolSize, keepAliveTime, keepAliveTimeUnit, threadLifeTime, contextService, rejectPolicy, controlPoint, processStateNotifier);
        }
        return managedExecutorService;
    }

    @Override
    public String toString() {
        return "Concurro 3.1";
    }
}
