package org.jboss.as.threads;

import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.ADD;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.REMOVE;
import static org.jboss.as.threads.CommonAttributes.BOUNDED_QUEUE_THREAD_POOL;
import static org.jboss.as.threads.CommonAttributes.GROUP_NAME;
import static org.jboss.as.threads.CommonAttributes.PRIORITY;
import static org.jboss.as.threads.CommonAttributes.QUEUELESS_THREAD_POOL;
import static org.jboss.as.threads.CommonAttributes.SCHEDULED_THREAD_POOL;
import static org.jboss.as.threads.CommonAttributes.THREAD_FACTORY;
import static org.jboss.as.threads.CommonAttributes.THREAD_NAME_PATTERN;
import static org.jboss.as.threads.CommonAttributes.UNBOUNDED_QUEUE_THREAD_POOL;
import static org.jboss.as.threads.ThreadsSubsystemProviders.BOUNDED_QUEUE_THREAD_POOL_DESC;
import static org.jboss.as.threads.ThreadsSubsystemProviders.QUEUELESS_THREAD_POOL_DESC;
import static org.jboss.as.threads.ThreadsSubsystemProviders.SCHEDULED_THREAD_POOL_DESC;
import static org.jboss.as.threads.ThreadsSubsystemProviders.THREAD_FACTORY_DESC;
import static org.jboss.as.threads.ThreadsSubsystemProviders.UNBOUNDED_QUEUE_THREAD_POOL_DESC;

import org.jboss.as.controller.PathElement;
import org.jboss.as.controller.registry.AttributeAccess.Storage;
import org.jboss.as.controller.registry.ModelNodeRegistration;

public class NewThreadsUtils {

    public static void registerOperations(final ModelNodeRegistration subsystem) {
        final ModelNodeRegistration threadFactories = subsystem.registerSubModel(PathElement.pathElement(THREAD_FACTORY),
                THREAD_FACTORY_DESC);
        threadFactories.registerOperationHandler(ADD, ThreadFactoryAdd.INSTANCE, ThreadFactoryAdd.INSTANCE, false);
        threadFactories.registerOperationHandler(REMOVE, ThreadFactoryRemove.INSTANCE, ThreadFactoryRemove.INSTANCE, false);
        threadFactories.registerReadWriteAttribute(THREAD_NAME_PATTERN, null, ThreadFactoryThreadNamePatternUpdate.INSTANCE,
                Storage.CONFIGURATION);
        threadFactories.registerReadWriteAttribute(GROUP_NAME, null, ThreadFactoryGroupNameUpdate.INSTANCE,
                Storage.CONFIGURATION);
        threadFactories.registerReadWriteAttribute(PRIORITY, null, ThreadFactoryPriorityUpdate.INSTANCE, Storage.CONFIGURATION);

        final ModelNodeRegistration boundedQueueThreadPools = subsystem.registerSubModel(
                PathElement.pathElement(BOUNDED_QUEUE_THREAD_POOL), BOUNDED_QUEUE_THREAD_POOL_DESC);
        boundedQueueThreadPools.registerOperationHandler(ADD, BoundedQueueThreadPoolAdd.INSTANCE,
                BoundedQueueThreadPoolAdd.INSTANCE, false);
        boundedQueueThreadPools.registerOperationHandler(REMOVE, BoundedQueueThreadPoolRemove.INSTANCE,
                BoundedQueueThreadPoolRemove.INSTANCE, false);

        final ModelNodeRegistration unboundedQueueThreadPools = subsystem.registerSubModel(
                PathElement.pathElement(UNBOUNDED_QUEUE_THREAD_POOL), UNBOUNDED_QUEUE_THREAD_POOL_DESC);
        unboundedQueueThreadPools.registerOperationHandler(ADD, UnboundedQueueThreadPoolAdd.INSTANCE,
                UnboundedQueueThreadPoolAdd.INSTANCE, false);
        unboundedQueueThreadPools.registerOperationHandler(REMOVE, UnboundedQueueThreadPoolRemove.INSTANCE,
                UnboundedQueueThreadPoolRemove.INSTANCE, false);

        final ModelNodeRegistration queuelessThreadPools = subsystem.registerSubModel(
                PathElement.pathElement(QUEUELESS_THREAD_POOL), QUEUELESS_THREAD_POOL_DESC);
        queuelessThreadPools.registerOperationHandler(ADD, QueuelessThreadPoolAdd.INSTANCE, QueuelessThreadPoolAdd.INSTANCE,
                false);
        queuelessThreadPools.registerOperationHandler(REMOVE, QueuelessThreadPoolRemove.INSTANCE,
                QueuelessThreadPoolRemove.INSTANCE, false);

        final ModelNodeRegistration scheduledThreadPools = subsystem.registerSubModel(
                PathElement.pathElement(SCHEDULED_THREAD_POOL), SCHEDULED_THREAD_POOL_DESC);
        scheduledThreadPools.registerOperationHandler(ADD, ScheduledThreadPoolAdd.INSTANCE, ScheduledThreadPoolAdd.INSTANCE,
                false);
        scheduledThreadPools.registerOperationHandler(REMOVE, ScheduledThreadPoolRemove.INSTANCE,
                ScheduledThreadPoolRemove.INSTANCE, false);
    }

}
