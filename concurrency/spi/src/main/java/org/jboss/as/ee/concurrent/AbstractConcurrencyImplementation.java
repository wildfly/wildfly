/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.jboss.as.ee.concurrent;

import org.jboss.as.controller.ExtensionContext;
import org.jboss.as.controller.OperationContext;
import org.jboss.as.controller.PathAddress;
import org.jboss.as.controller.registry.ManagementResourceRegistration;
import org.jboss.as.ee.concurrent.deployers.ConcurrencyResourceReferenceRegistryProcessor;
import org.jboss.as.ee.concurrent.deployers.EEConcurrentContextProcessor;
import org.jboss.as.ee.concurrent.deployers.EEConcurrentDefaultBindingProcessor;
import org.jboss.as.ee.concurrent.resource.definition.ContextServiceDefinitionAnnotationProcessor;
import org.jboss.as.ee.concurrent.resource.definition.ContextServiceDefinitionDescriptorProcessor;
import org.jboss.as.ee.concurrent.resource.definition.ManagedExecutorDefinitionAnnotationProcessor;
import org.jboss.as.ee.concurrent.resource.definition.ManagedExecutorDefinitionDescriptorProcessor;
import org.jboss.as.ee.concurrent.resource.definition.ManagedScheduledExecutorDefinitionAnnotationProcessor;
import org.jboss.as.ee.concurrent.resource.definition.ManagedScheduledExecutorDefinitionDescriptorProcessor;
import org.jboss.as.ee.concurrent.resource.definition.ManagedThreadFactoryDefinitionAnnotationProcessor;
import org.jboss.as.ee.concurrent.resource.definition.ManagedThreadFactoryDefinitionDescriptorProcessor;
import org.jboss.as.ee.concurrent.service.ManagedExecutorHungTasksPeriodicTerminationService;
import org.jboss.as.ee.subsystem.ConcurrentEESubsystemParser20;
import org.jboss.as.ee.subsystem.ConcurrentEESubsystemParser40;
import org.jboss.as.ee.subsystem.ConcurrentEESubsystemParser50;
import org.jboss.as.ee.subsystem.ConcurrentEESubsystemParser60;
import org.jboss.as.ee.subsystem.ConcurrentEESubsystemParser70;
import org.jboss.as.ee.subsystem.ConcurrentEESubsystemXMLPersister;
import org.jboss.as.ee.subsystem.ContextServiceResourceDefinition;
import org.jboss.as.ee.subsystem.EeExtension;
import org.jboss.as.ee.subsystem.ManagedExecutorServiceResourceDefinition;
import org.jboss.as.ee.subsystem.ManagedScheduledExecutorServiceResourceDefinition;
import org.jboss.as.ee.subsystem.ManagedThreadFactoryResourceDefinition;
import org.jboss.as.ee.subsystem.Namespace;
import org.jboss.as.server.DeploymentProcessorTarget;
import org.jboss.as.server.deployment.Phase;
import org.jboss.dmr.ModelNode;
import org.jboss.staxmapper.XMLExtendedStreamReader;
import org.jboss.staxmapper.XMLExtendedStreamWriter;

import javax.xml.stream.XMLStreamException;
import java.util.List;

/**
 * @author Eduardo Martins
 */
public abstract class AbstractConcurrencyImplementation implements ConcurrencyImplementation {
    @Override
    public void addDeploymentProcessors(DeploymentProcessorTarget processorTarget) {
        // TODO get a new Phase.STRUCTURE_ int for this new DUP
        processorTarget.addDeploymentProcessor(EeExtension.SUBSYSTEM_NAME, Phase.STRUCTURE, Phase.STRUCTURE_BEAN_VALIDATION_RESOURCE_INJECTION_REGISTRY, new ConcurrencyResourceReferenceRegistryProcessor());
        processorTarget.addDeploymentProcessor(EeExtension.SUBSYSTEM_NAME, Phase.PARSE, Phase.PARSE_RESOURCE_DEF_ANNOTATION_DATA_SOURCE, new ContextServiceDefinitionAnnotationProcessor());
        processorTarget.addDeploymentProcessor(EeExtension.SUBSYSTEM_NAME, Phase.PARSE, Phase.PARSE_RESOURCE_DEF_ANNOTATION_DATA_SOURCE, new ManagedExecutorDefinitionAnnotationProcessor());
        processorTarget.addDeploymentProcessor(EeExtension.SUBSYSTEM_NAME, Phase.PARSE, Phase.PARSE_RESOURCE_DEF_ANNOTATION_DATA_SOURCE, new ManagedScheduledExecutorDefinitionAnnotationProcessor());
        processorTarget.addDeploymentProcessor(EeExtension.SUBSYSTEM_NAME, Phase.PARSE, Phase.PARSE_RESOURCE_DEF_ANNOTATION_DATA_SOURCE, new ManagedThreadFactoryDefinitionAnnotationProcessor());
        processorTarget.addDeploymentProcessor(EeExtension.SUBSYSTEM_NAME, Phase.POST_MODULE, Phase.POST_MODULE_EE_CONCURRENT_CONTEXT, new EEConcurrentContextProcessor());
        // TODO create Phase priorities
        processorTarget.addDeploymentProcessor(EeExtension.SUBSYSTEM_NAME, Phase.POST_MODULE, Phase.POST_MODULE_RESOURCE_DEF_XML_DATA_SOURCE, new ContextServiceDefinitionDescriptorProcessor());
        processorTarget.addDeploymentProcessor(EeExtension.SUBSYSTEM_NAME, Phase.POST_MODULE, Phase.POST_MODULE_RESOURCE_DEF_XML_DATA_SOURCE, new ManagedExecutorDefinitionDescriptorProcessor());
        processorTarget.addDeploymentProcessor(EeExtension.SUBSYSTEM_NAME, Phase.POST_MODULE, Phase.POST_MODULE_RESOURCE_DEF_XML_DATA_SOURCE, new ManagedScheduledExecutorDefinitionDescriptorProcessor());
        processorTarget.addDeploymentProcessor(EeExtension.SUBSYSTEM_NAME, Phase.POST_MODULE, Phase.POST_MODULE_RESOURCE_DEF_XML_DATA_SOURCE, new ManagedThreadFactoryDefinitionDescriptorProcessor());
        processorTarget.addDeploymentProcessor(EeExtension.SUBSYSTEM_NAME, Phase.INSTALL, Phase.INSTALL_DEFAULT_BINDINGS_EE_CONCURRENCY, new EEConcurrentDefaultBindingProcessor());
    }

    @Override
    public void installSubsystemServices(OperationContext context) {
        // installs the service which manages managed executor's hung task periodic termination
        new ManagedExecutorHungTasksPeriodicTerminationService().install(context);
    }

    @Override
    public void registerRootResourceSubModels(ManagementResourceRegistration rootResource, ExtensionContext context) {
        final boolean runtimeOnlyRegistrationValid = context.isRuntimeOnlyRegistrationValid();
        rootResource.registerSubModel(new ContextServiceResourceDefinition());
        rootResource.registerSubModel(new ManagedThreadFactoryResourceDefinition());
        rootResource.registerSubModel(new ManagedExecutorServiceResourceDefinition(runtimeOnlyRegistrationValid));
        rootResource.registerSubModel(new ManagedScheduledExecutorServiceResourceDefinition(runtimeOnlyRegistrationValid));
    }

    @Override
    public void parseConcurrentElement(Namespace namespace, XMLExtendedStreamReader reader, List<ModelNode> operations, PathAddress subsystemPathAddress) throws XMLStreamException {
        switch (namespace) {
            case UNKNOWN -> throw new IllegalArgumentException("Unknown namespace");
            case EE_1_0, EE_1_1, EE_1_2 ->
                    throw new IllegalArgumentException("Concurrent element was only introduced with namespace "+Namespace.EE_2_0);
            case EE_2_0, EE_3_0 ->
                    ConcurrentEESubsystemParser20.parseConcurrent(reader, operations, subsystemPathAddress);
            case EE_4_0 -> ConcurrentEESubsystemParser40.parseConcurrent(reader, operations, subsystemPathAddress);
            case EE_5_0 -> ConcurrentEESubsystemParser50.parseConcurrent(reader, operations, subsystemPathAddress);
            case EE_6_0 -> ConcurrentEESubsystemParser60.parseConcurrent(reader, operations, subsystemPathAddress);
            case EE_7_0 -> ConcurrentEESubsystemParser70.parseConcurrent(reader, operations, subsystemPathAddress);
        }
    }

    @Override
    public void writeConcurrentElement(XMLExtendedStreamWriter writer, ModelNode eeSubSystem) throws XMLStreamException {
        ConcurrentEESubsystemXMLPersister.writeConcurrentElement(writer, eeSubSystem);
    }
}
