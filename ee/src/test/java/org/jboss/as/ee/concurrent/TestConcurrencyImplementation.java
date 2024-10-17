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
 * @author emartins
 */
public class TestConcurrencyImplementation implements ConcurrencyImplementation {

    @Override
    public void registerRootResourceSubModels(ManagementResourceRegistration rootResource, ExtensionContext context) {

    }

    @Override
    public void addDeploymentProcessors(DeploymentProcessorTarget processorTarget) {

    }

    @Override
    public void installSubsystemServices(OperationContext context) {

    }

    @Override
    public void parseConcurrentElement20(XMLExtendedStreamReader reader, List<ModelNode> operations, PathAddress subsystemPathAddress) throws XMLStreamException {

    }

    @Override
    public void parseConcurrentElement40(XMLExtendedStreamReader reader, List<ModelNode> operations, PathAddress subsystemPathAddress) throws XMLStreamException {

    }

    @Override
    public void parseConcurrentElement50(XMLExtendedStreamReader reader, List<ModelNode> operations, PathAddress subsystemPathAddress) throws XMLStreamException {

    }

    @Override
    public void parseConcurrentElement60(XMLExtendedStreamReader reader, List<ModelNode> operations, PathAddress subsystemPathAddress) throws XMLStreamException {

    }

    @Override
    public void writeConcurrentElement(XMLExtendedStreamWriter writer, ModelNode eeSubSystem) throws XMLStreamException {

    }

    @Override
    public WildflyContextService newContextService(String name, ContextServiceTypesConfiguration contextServiceTypesConfiguration) {
        return null;
    }

    @Override
    public WildFlyManagedThreadFactory newManagedThreadFactory(String name, WildflyContextService contextService, int priority) {
        return null;
    }

    @Override
    public WildflyManagedExecutorService newManagedExecutorService(String name, WildFlyManagedThreadFactory managedThreadFactory, long hungTaskThreshold, boolean longRunningTasks, int corePoolSize, int maxPoolSize, long keepAliveTime, TimeUnit keepAliveTimeUnit, long threadLifeTime, WildflyContextService contextService, WildflyManagedExecutorService.RejectPolicy rejectPolicy, BlockingQueue<Runnable> queue, ControlPoint controlPoint, ProcessStateNotifier processStateNotifier) {
        return null;
    }

    @Override
    public WildflyManagedExecutorService newManagedExecutorService(String name, WildFlyManagedThreadFactory managedThreadFactory, long hungTaskThreshold, boolean longRunningTasks, int corePoolSize, int maxPoolSize, long keepAliveTime, TimeUnit keepAliveTimeUnit, long threadLifeTime, int queueCapacity, WildflyContextService contextService, WildflyManagedExecutorService.RejectPolicy rejectPolicy, ControlPoint controlPoint, ProcessStateNotifier processStateNotifier) {
        return null;
    }

    @Override
    public WildflyManagedScheduledExecutorService newManagedScheduledExecutorService(String name, WildFlyManagedThreadFactory managedThreadFactory, long hungTaskThreshold, boolean longRunningTasks, int corePoolSize, long keepAliveTime, TimeUnit keepAliveTimeUnit, long threadLifeTime, WildflyContextService wildflyContextService, WildflyManagedExecutorService.RejectPolicy rejectPolicy, ControlPoint controlPoint, ProcessStateNotifier processStateNotifier) {
        return null;
    }
}