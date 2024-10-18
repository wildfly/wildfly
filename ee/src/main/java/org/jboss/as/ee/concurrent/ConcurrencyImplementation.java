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
    void registerRootResourceSubModels(ManagementResourceRegistration rootResource, ExtensionContext context);

    // subsystem add ops
    void addDeploymentProcessors(DeploymentProcessorTarget processorTarget);
    void installSubsystemServices(OperationContext context);

    // XML config ops
    void parseConcurrentElement20(XMLExtendedStreamReader reader, List<ModelNode> operations, PathAddress subsystemPathAddress) throws XMLStreamException;
    void parseConcurrentElement40(XMLExtendedStreamReader reader, List<ModelNode> operations, PathAddress subsystemPathAddress) throws XMLStreamException;
    void parseConcurrentElement50(XMLExtendedStreamReader reader, List<ModelNode> operations, PathAddress subsystemPathAddress) throws XMLStreamException;
    void parseConcurrentElement60(XMLExtendedStreamReader reader, List<ModelNode> operations, PathAddress subsystemPathAddress) throws XMLStreamException;
    void writeConcurrentElement(XMLExtendedStreamWriter writer, ModelNode eeSubSystem) throws XMLStreamException;

    WildflyContextService newContextService(String name, ContextServiceTypesConfiguration contextServiceTypesConfiguration);
    WildFlyManagedThreadFactory newManagedThreadFactory(String name, WildflyContextService contextService, int priority);
    WildflyManagedExecutorService newManagedExecutorService(String name, WildFlyManagedThreadFactory managedThreadFactory, long hungTaskThreshold, boolean longRunningTasks, int corePoolSize, int maxPoolSize, long keepAliveTime, TimeUnit keepAliveTimeUnit, long threadLifeTime, WildflyContextService contextService, WildflyManagedExecutorService.RejectPolicy rejectPolicy, BlockingQueue<Runnable> queue, ControlPoint controlPoint, ProcessStateNotifier processStateNotifier);
    WildflyManagedExecutorService newManagedExecutorService(String name, WildFlyManagedThreadFactory managedThreadFactory, long hungTaskThreshold, boolean longRunningTasks, int corePoolSize, int maxPoolSize, long keepAliveTime, TimeUnit keepAliveTimeUnit, long threadLifeTime, int queueCapacity, WildflyContextService contextService, WildflyManagedExecutorService.RejectPolicy rejectPolicy, ControlPoint controlPoint, ProcessStateNotifier processStateNotifier);
    WildflyManagedScheduledExecutorService newManagedScheduledExecutorService(String name, WildFlyManagedThreadFactory managedThreadFactory, long hungTaskThreshold, boolean longRunningTasks, int corePoolSize, long keepAliveTime, TimeUnit keepAliveTimeUnit, long threadLifeTime, WildflyContextService wildflyContextService, WildflyManagedExecutorService.RejectPolicy rejectPolicy, ControlPoint controlPoint, ProcessStateNotifier processStateNotifier);
}
