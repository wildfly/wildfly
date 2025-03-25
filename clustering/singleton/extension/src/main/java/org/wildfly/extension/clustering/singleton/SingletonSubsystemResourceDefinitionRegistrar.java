/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.wildfly.extension.clustering.singleton;

import java.util.EnumSet;
import java.util.List;
import java.util.function.Consumer;

import org.jboss.as.controller.OperationContext;
import org.jboss.as.controller.OperationFailedException;
import org.jboss.as.controller.ResourceDefinition;
import org.jboss.as.controller.ServiceNameFactory;
import org.jboss.as.controller.SubsystemRegistration;
import org.jboss.as.controller.SubsystemResourceRegistration;
import org.jboss.as.controller.capability.RuntimeCapability;
import org.jboss.as.controller.descriptions.ModelDescriptionConstants;
import org.jboss.as.controller.descriptions.ParentResourceDescriptionResolver;
import org.jboss.as.controller.descriptions.SubsystemResourceDescriptionResolver;
import org.jboss.as.controller.registry.ManagementResourceRegistration;
import org.jboss.as.server.DeploymentProcessorTarget;
import org.jboss.as.server.deployment.Phase;
import org.jboss.as.server.deployment.jbossallxml.JBossAllSchema;
import org.jboss.dmr.ModelNode;
import org.wildfly.clustering.singleton.service.ServiceTargetFactory;
import org.wildfly.clustering.singleton.service.SingletonPolicy;
import org.wildfly.extension.clustering.singleton.deployment.SingletonDeploymentDependencyProcessor;
import org.wildfly.extension.clustering.singleton.deployment.SingletonDeploymentParsingProcessor;
import org.wildfly.extension.clustering.singleton.deployment.SingletonDeploymentProcessor;
import org.wildfly.extension.clustering.singleton.deployment.SingletonDeploymentSchema;
import org.wildfly.subsystem.resource.ManagementResourceRegistrar;
import org.wildfly.subsystem.resource.ManagementResourceRegistrationContext;
import org.wildfly.subsystem.resource.ResourceDescriptor;
import org.wildfly.subsystem.resource.SubsystemResourceDefinitionRegistrar;
import org.wildfly.subsystem.resource.capability.CapabilityReference;
import org.wildfly.subsystem.resource.capability.CapabilityReferenceAttributeDefinition;
import org.wildfly.subsystem.resource.operation.ResourceOperationRuntimeHandler;
import org.wildfly.subsystem.service.ResourceServiceConfigurator;
import org.wildfly.subsystem.service.ResourceServiceInstaller;
import org.wildfly.subsystem.service.capability.CapabilityServiceInstaller;

/**
 * Registers a resource definition for the singleton subsystem.
 * @author Paul Ferraro
 */
public class SingletonSubsystemResourceDefinitionRegistrar implements SubsystemResourceDefinitionRegistrar, ResourceServiceConfigurator, Consumer<DeploymentProcessorTarget> {

    static final SubsystemResourceRegistration REGISTRATION = SubsystemResourceRegistration.of("singleton");
    static final ParentResourceDescriptionResolver RESOLVER = new SubsystemResourceDescriptionResolver(REGISTRATION.getName(), SingletonExtension.class);

    private static final RuntimeCapability<Void> CAPABILITY = RuntimeCapability.Builder.of(ServiceTargetFactory.DEFAULT_SERVICE_DESCRIPTOR).build();

    static final CapabilityReferenceAttributeDefinition<ServiceTargetFactory> DEFAULT_SERVICE_TARGET_FACTORY = new CapabilityReferenceAttributeDefinition.Builder<>(ModelDescriptionConstants.DEFAULT, CapabilityReference.builder(CAPABILITY, ServiceTargetFactory.SERVICE_DESCRIPTOR).build()).build();

    @Override
    public ManagementResourceRegistration register(SubsystemRegistration parent, ManagementResourceRegistrationContext context) {
        ResourceDescriptor descriptor = ResourceDescriptor.builder(RESOLVER)
                .addCapability(CAPABILITY)
                .addAttributes(List.of(DEFAULT_SERVICE_TARGET_FACTORY))
                .withRuntimeHandler(ResourceOperationRuntimeHandler.configureService(this))
                .withDeploymentChainContributor(this)
                .build();
        ManagementResourceRegistration registration = parent.registerSubsystemModel(ResourceDefinition.builder(REGISTRATION, RESOLVER).build());
        ManagementResourceRegistrar.of(descriptor).register(registration);

        new SingletonPolicyResourceDefinitionRegistrar().register(registration, context);

        return registration;
    }

    @Override
    public void accept(DeploymentProcessorTarget target) {
        target.addDeploymentProcessor(REGISTRATION.getName(), Phase.STRUCTURE, Phase.STRUCTURE_REGISTER_JBOSS_ALL_SINGLETON_DEPLOYMENT, JBossAllSchema.createDeploymentUnitProcessor(EnumSet.allOf(SingletonDeploymentSchema.class), SingletonDeploymentDependencyProcessor.CONFIGURATION_KEY));
        target.addDeploymentProcessor(REGISTRATION.getName(), Phase.PARSE, Phase.PARSE_SINGLETON_DEPLOYMENT, new SingletonDeploymentParsingProcessor());
        target.addDeploymentProcessor(REGISTRATION.getName(), Phase.DEPENDENCIES, Phase.DEPENDENCIES_SINGLETON_DEPLOYMENT, new SingletonDeploymentDependencyProcessor());
        target.addDeploymentProcessor(REGISTRATION.getName(), Phase.CONFIGURE_MODULE, Phase.CONFIGURE_SINGLETON_DEPLOYMENT, new SingletonDeploymentProcessor());
    }

    @Override
    public ResourceServiceInstaller configure(OperationContext context, ModelNode model) throws OperationFailedException {
        return CapabilityServiceInstaller.builder(CAPABILITY, DEFAULT_SERVICE_TARGET_FACTORY.resolve(context, model))
                .provides(ServiceNameFactory.resolveServiceName(SingletonPolicy.DEFAULT_SERVICE_DESCRIPTOR))
                .provides(ServiceNameFactory.resolveServiceName(org.wildfly.clustering.singleton.SingletonPolicy.DEFAULT_SERVICE_DESCRIPTOR))
                .build();
    }
}
