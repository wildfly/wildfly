package org.jboss.as.ee.concurrent;

import org.jboss.as.controller.ProcessStateNotifier;
import org.wildfly.extension.requestcontroller.ControlPoint;

import java.util.concurrent.BlockingQueue;
import java.util.concurrent.TimeUnit;

public class ConcurrencyImplementation30 extends AbstractConcurrencyImplementation {

    @Override
    public WildflyContextService newContextService(String name, ContextServiceTypesConfiguration contextServiceTypesConfiguration) {
        return new ContextServiceImpl(name, contextServiceTypesConfiguration);
    }

    @Override
    public WildFlyManagedThreadFactory newManagedThreadFactory(String name, WildflyContextService contextService, int priority) {
        return new ManagedThreadFactoryImpl(name, contextService, priority);
    }

    @Override
    public WildflyManagedExecutorService newManagedExecutorService(String name, WildFlyManagedThreadFactory managedThreadFactory, long hungTaskThreshold, boolean longRunningTasks, int corePoolSize, int maxPoolSize, long keepAliveTime, TimeUnit keepAliveTimeUnit, long threadLifeTime, WildflyContextService contextService, WildflyManagedExecutorService.RejectPolicy rejectPolicy, BlockingQueue<Runnable> queue, ControlPoint controlPoint, ProcessStateNotifier processStateNotifier) {
        return new ManagedExecutorServiceImpl(name, managedThreadFactory, hungTaskThreshold, longRunningTasks, corePoolSize, maxPoolSize, keepAliveTime, keepAliveTimeUnit, threadLifeTime, contextService, rejectPolicy, queue, controlPoint, processStateNotifier);
    }

    @Override
    public WildflyManagedExecutorService newManagedExecutorService(String name, WildFlyManagedThreadFactory managedThreadFactory, long hungTaskThreshold, boolean longRunningTasks, int corePoolSize, int maxPoolSize, long keepAliveTime, TimeUnit keepAliveTimeUnit, long threadLifeTime, int queueCapacity, WildflyContextService contextService, WildflyManagedExecutorService.RejectPolicy rejectPolicy, ControlPoint controlPoint, ProcessStateNotifier processStateNotifier) {
        return new ManagedExecutorServiceImpl(name, managedThreadFactory, hungTaskThreshold, longRunningTasks, corePoolSize, maxPoolSize, keepAliveTime, keepAliveTimeUnit, threadLifeTime, queueCapacity, contextService, rejectPolicy, controlPoint, processStateNotifier);
    }

    @Override
    public WildflyManagedScheduledExecutorService newManagedScheduledExecutorService(String name, WildFlyManagedThreadFactory managedThreadFactory, long hungTaskThreshold, boolean longRunningTasks, int corePoolSize, long keepAliveTime, TimeUnit keepAliveTimeUnit, long threadLifeTime, WildflyContextService contextService, WildflyManagedExecutorService.RejectPolicy rejectPolicy, ControlPoint controlPoint, ProcessStateNotifier processStateNotifier) {
        return new ManagedScheduledExecutorServiceImpl(name, managedThreadFactory, hungTaskThreshold, longRunningTasks, corePoolSize, keepAliveTime, keepAliveTimeUnit, threadLifeTime, contextService, rejectPolicy, controlPoint, processStateNotifier);
    }

    @Override
    public String toString() {
        return "Concurrency RI 3.0";
    }
}
