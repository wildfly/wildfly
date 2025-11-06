/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.jboss.as.ee.concurrent;

import org.jboss.as.controller.ExtensionContext;
import org.jboss.as.controller.OperationContext;
import org.jboss.as.controller.PathAddress;
import org.jboss.as.controller.ProcessStateNotifier;
import org.jboss.as.controller.registry.ManagementResourceRegistration;
import org.jboss.as.server.DeploymentProcessorTarget;
import org.jboss.dmr.ModelNode;
import org.jboss.staxmapper.XMLExtendedStreamReader;
import org.jboss.staxmapper.XMLExtendedStreamWriter;
import org.wildfly.extension.requestcontroller.ControlPoint;

import javax.xml.stream.XMLStreamException;
import java.util.List;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.TimeUnit;


/**
 * The interface for a Jakarta Concurrency Implementation
 * @author Eduardo Martins
 */
public interface ConcurrencyImplementation {

    ConcurrencyImplementation INSTANCE = ConcurrencyImplementationLoader.load();

    // extension init ops

    /**
     * Register the existence of the concurrency related sub-resources, with the provided root resource.
     * @param rootResource the root resource to register concurrency sub-resources
     * @param context the context of the root resource's extension
     */
    void registerRootResourceSubModels(ManagementResourceRegistration rootResource, ExtensionContext context);

    // subsystem add ops

    /**
     * Adds the concurrency related deployment unit processors.
     * @param processorTarget the processor target to add deployment unit processors
     */
    void addDeploymentProcessors(DeploymentProcessorTarget processorTarget);

    /**
     *
     * @return the name of the JBoss Module for the Concurrency Implementation
     */
    String getJBossModuleName();

    /**
     * Installs boot-time concurrency related subsystem services
     * @param context the boot-time subsystem add operation context
     */
    void installSubsystemServices(OperationContext context);

    // XML config ops

    /**
     * Parses <concurrent/> XML element for EE subsystem schema 2.0
     * @param reader the stream reader
     * @param operations the list to add operations
     * @param subsystemPathAddress the subsystem path address
     * @throws XMLStreamException if an error occurs
     */
    void parseConcurrentElement20(XMLExtendedStreamReader reader, List<ModelNode> operations, PathAddress subsystemPathAddress) throws XMLStreamException;

    /**
     * Parses <concurrent/> XML element for EE subsystem schema 4.0
     * @param reader the stream reader
     * @param operations the list to add operations
     * @param subsystemPathAddress the subsystem path address
     * @throws XMLStreamException if an error occurs
     */
    void parseConcurrentElement40(XMLExtendedStreamReader reader, List<ModelNode> operations, PathAddress subsystemPathAddress) throws XMLStreamException;

    /**
     * Parses <concurrent/> XML element for EE subsystem schema 5.0
     * @param reader the stream reader
     * @param operations the list to add operations
     * @param subsystemPathAddress the subsystem path address
     * @throws XMLStreamException if an error occurs
     */
    void parseConcurrentElement50(XMLExtendedStreamReader reader, List<ModelNode> operations, PathAddress subsystemPathAddress) throws XMLStreamException;

    /**
     * Parses <concurrent/> XML element for EE subsystem schema 6.0
     * @param reader the stream reader
     * @param operations the list to add operations
     * @param subsystemPathAddress the subsystem path address
     * @throws XMLStreamException if an error occurs
     */
    void parseConcurrentElement60(XMLExtendedStreamReader reader, List<ModelNode> operations, PathAddress subsystemPathAddress) throws XMLStreamException;

    /**
     * Writes <concurrent/> XML element.
     * @param writer the stream writer
     * @param eeSubSystem the model note to write
     * @throws XMLStreamException if an error occurs
     */
    void writeConcurrentElement(XMLExtendedStreamWriter writer, ModelNode eeSubSystem) throws XMLStreamException;

    /**
     * @param name the instance name
     * @param contextServiceTypesConfiguration the context service's types configuration
     * @return a new WildFlyContextService instance with the specified params.
     */
    WildFlyContextService newContextService(String name, ContextServiceTypesConfiguration contextServiceTypesConfiguration);

    /**
     * @param name the instance name
     * @param contextService the context service to use
     * @param priority the thread factory priority
     * @return a new WildFlyManagedThreadFactory instance with the specified params.
     */
    WildFlyManagedThreadFactory newManagedThreadFactory(String name, WildFlyContextService contextService, int priority);

    /**
     *
     * @param name the instance name
     * @param managedThreadFactory the thread factory to use
     * @param hungTaskThreshold the hung task threshold
     * @param longRunningTasks flag which hints the duration of tasks executed by the executor.
     * @param corePoolSize the core thread-pool size to use
     * @param maxPoolSize the max thread-pool size to use
     * @param keepAliveTime the thread keep-alive time
     * @param keepAliveTimeUnit the thread keep-alive time unit
     * @param threadLifeTime the thread lifetime
     * @param contextService the context service to use
     * @param rejectPolicy the policy to be applied to aborted tasks.
     * @param queue the task queue
     * @param controlPoint the control point
     * @param processStateNotifier the process state notifier
     * @return a new WildFlyManagedExecutorService instance with the specified params.
     */
    WildFlyManagedExecutorService newManagedExecutorService(String name, WildFlyManagedThreadFactory managedThreadFactory, long hungTaskThreshold, boolean longRunningTasks, int corePoolSize, int maxPoolSize, long keepAliveTime, TimeUnit keepAliveTimeUnit, long threadLifeTime, WildFlyContextService contextService, WildFlyManagedExecutorService.RejectPolicy rejectPolicy, BlockingQueue<Runnable> queue, ControlPoint controlPoint, ProcessStateNotifier processStateNotifier);

    /**
     *
     * @param name the instance name
     * @param managedThreadFactory the thread factory to use
     * @param hungTaskThreshold the hung task threshold
     * @param longRunningTasks flag which hints the duration of tasks executed by the executor.
     * @param corePoolSize the core thread-pool size to use
     * @param maxPoolSize the max thread-pool size to use
     * @param keepAliveTime the thread keep-alive time
     * @param keepAliveTimeUnit the thread keep-alive time unit
     * @param threadLifeTime the thread lifetime
     * @param queueCapacity the task queue capacity
     * @param contextService the context service to use
     * @param rejectPolicy the policy to be applied to aborted tasks.
     * @param controlPoint the control point
     * @param processStateNotifier the process state notifier
     * @return a new WildFlyManagedExecutorService instance with the specified params.
     */
    WildFlyManagedExecutorService newManagedExecutorService(String name, WildFlyManagedThreadFactory managedThreadFactory, long hungTaskThreshold, boolean longRunningTasks, int corePoolSize, int maxPoolSize, long keepAliveTime, TimeUnit keepAliveTimeUnit, long threadLifeTime, int queueCapacity, WildFlyContextService contextService, WildFlyManagedExecutorService.RejectPolicy rejectPolicy, ControlPoint controlPoint, ProcessStateNotifier processStateNotifier);

    /**
     *
     * @param name the instance name
     * @param managedThreadFactory the thread factory to use
     * @param hungTaskThreshold the hung task threshold
     * @param longRunningTasks flag which hints the duration of tasks executed by the executor.
     * @param corePoolSize the core thread-pool size to use
     * @param keepAliveTime the thread keep-alive time
     * @param keepAliveTimeUnit the thread keep-alive time unit
     * @param threadLifeTime the thread lifetime
     * @param contextService the context service to use
     * @param rejectPolicy the policy to be applied to aborted tasks.
     * @param controlPoint the control point
     * @param processStateNotifier the process state notifier
     * @return a new WildFlyManagedScheduledExecutorService instance with the specified params.
     */
    WildFlyManagedScheduledExecutorService newManagedScheduledExecutorService(String name, WildFlyManagedThreadFactory managedThreadFactory, long hungTaskThreshold, boolean longRunningTasks, int corePoolSize, long keepAliveTime, TimeUnit keepAliveTimeUnit, long threadLifeTime, WildFlyContextService contextService, WildFlyManagedExecutorService.RejectPolicy rejectPolicy, ControlPoint controlPoint, ProcessStateNotifier processStateNotifier);
}
