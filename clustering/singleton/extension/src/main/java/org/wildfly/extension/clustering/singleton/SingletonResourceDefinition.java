/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.wildfly.extension.clustering.singleton;

import java.util.EnumSet;
import java.util.List;
import java.util.function.Consumer;

import org.jboss.as.clustering.controller.DeploymentChainContributingResourceRegistrar;
import org.jboss.as.clustering.controller.ManagementResourceRegistration;
import org.jboss.as.clustering.controller.ResourceDescriptor;
import org.jboss.as.clustering.controller.ResourceServiceHandler;
import org.jboss.as.clustering.controller.SubsystemRegistration;
import org.jboss.as.clustering.controller.SubsystemResourceDefinition;
import org.jboss.as.controller.OperationContext;
import org.jboss.as.controller.OperationFailedException;
import org.jboss.as.controller.PathElement;
import org.jboss.as.controller.ServiceNameFactory;
import org.jboss.as.controller.SimpleAttributeDefinition;
import org.jboss.as.controller.SimpleAttributeDefinitionBuilder;
import org.jboss.as.controller.capability.RuntimeCapability;
import org.jboss.as.controller.operations.common.GenericSubsystemDescribeHandler;
import org.jboss.as.controller.registry.AttributeAccess.Flag;
import org.jboss.as.server.DeploymentProcessorTarget;
import org.jboss.as.server.deployment.Phase;
import org.jboss.as.server.deployment.jbossallxml.JBossAllSchema;
import org.jboss.dmr.ModelNode;
import org.jboss.dmr.ModelType;
import org.wildfly.clustering.singleton.service.ServiceTargetFactory;
import org.wildfly.clustering.singleton.service.SingletonPolicy;
import org.wildfly.extension.clustering.singleton.deployment.SingletonDeploymentDependencyProcessor;
import org.wildfly.extension.clustering.singleton.deployment.SingletonDeploymentParsingProcessor;
import org.wildfly.extension.clustering.singleton.deployment.SingletonDeploymentProcessor;
import org.wildfly.extension.clustering.singleton.deployment.SingletonDeploymentSchema;
import org.wildfly.subsystem.resource.capability.CapabilityReferenceRecorder;
import org.wildfly.subsystem.resource.operation.ResourceOperationRuntimeHandler;
import org.wildfly.subsystem.service.ResourceServiceConfigurator;
import org.wildfly.subsystem.service.ResourceServiceInstaller;
import org.wildfly.subsystem.service.ServiceDependency;
import org.wildfly.subsystem.service.capability.CapabilityServiceInstaller;

/**
 * Definition of the singleton deployer resource.
 * @author Paul Ferraro
 */
public class SingletonResourceDefinition extends SubsystemResourceDefinition implements ResourceServiceConfigurator, Consumer<DeploymentProcessorTarget> {

    static final PathElement PATH = pathElement(SingletonExtension.SUBSYSTEM_NAME);

    private static final RuntimeCapability<Void> DEFAULT_SERVICE_TARGET_FACTORY = RuntimeCapability.Builder.of(ServiceTargetFactory.DEFAULT_SERVICE_DESCRIPTOR).build();

    enum Attribute implements org.jboss.as.clustering.controller.Attribute {
        DEFAULT("default", ModelType.STRING, CapabilityReferenceRecorder.builder(DEFAULT_SERVICE_TARGET_FACTORY, ServiceTargetFactory.SERVICE_DESCRIPTOR).build()),
        ;
        private final SimpleAttributeDefinition definition;

        Attribute(String name, ModelType type, CapabilityReferenceRecorder<?> reference) {
            this.definition = new SimpleAttributeDefinitionBuilder(name, type)
                    .setCapabilityReference(reference)
                    .setFlags(Flag.RESTART_RESOURCE_SERVICES)
                    .build();
        }

        @Override
        public SimpleAttributeDefinition getDefinition() {
            return this.definition;
        }
    }

    SingletonResourceDefinition() {
        super(PATH, SingletonExtension.SUBSYSTEM_RESOLVER);
    }

    @Override
    public void register(SubsystemRegistration parentRegistration) {
        ManagementResourceRegistration registration = parentRegistration.registerSubsystemModel(this);

        registration.registerOperationHandler(GenericSubsystemDescribeHandler.DEFINITION, GenericSubsystemDescribeHandler.INSTANCE);

        ResourceDescriptor descriptor = new ResourceDescriptor(this.getResourceDescriptionResolver())
                .addAttributes(Attribute.class)
                .addCapabilities(List.of(DEFAULT_SERVICE_TARGET_FACTORY))
                ;
        ResourceOperationRuntimeHandler handler = ResourceOperationRuntimeHandler.configureService(this);
        new DeploymentChainContributingResourceRegistrar(descriptor, ResourceServiceHandler.of(handler), this).register(registration);

        new SingletonPolicyResourceDefinition().register(registration);
    }

    @Override
    public void accept(DeploymentProcessorTarget target) {
        target.addDeploymentProcessor(SingletonExtension.SUBSYSTEM_NAME, Phase.STRUCTURE, Phase.STRUCTURE_REGISTER_JBOSS_ALL_SINGLETON_DEPLOYMENT, JBossAllSchema.createDeploymentUnitProcessor(EnumSet.allOf(SingletonDeploymentSchema.class), SingletonDeploymentDependencyProcessor.CONFIGURATION_KEY));
        target.addDeploymentProcessor(SingletonExtension.SUBSYSTEM_NAME, Phase.PARSE, Phase.PARSE_SINGLETON_DEPLOYMENT, new SingletonDeploymentParsingProcessor());
        target.addDeploymentProcessor(SingletonExtension.SUBSYSTEM_NAME, Phase.DEPENDENCIES, Phase.DEPENDENCIES_SINGLETON_DEPLOYMENT, new SingletonDeploymentDependencyProcessor());
        target.addDeploymentProcessor(SingletonExtension.SUBSYSTEM_NAME, Phase.CONFIGURE_MODULE, Phase.CONFIGURE_SINGLETON_DEPLOYMENT, new SingletonDeploymentProcessor());
    }

    @Override
    public ResourceServiceInstaller configure(OperationContext context, ModelNode model) throws OperationFailedException {
        String defaultPolicy = Attribute.DEFAULT.resolveModelAttribute(context, model).asString();
        return CapabilityServiceInstaller.builder(DEFAULT_SERVICE_TARGET_FACTORY, ServiceDependency.on(ServiceTargetFactory.SERVICE_DESCRIPTOR, defaultPolicy))
                .provides(ServiceNameFactory.resolveServiceName(SingletonPolicy.DEFAULT_SERVICE_DESCRIPTOR))
                .provides(ServiceNameFactory.resolveServiceName(org.wildfly.clustering.singleton.SingletonPolicy.DEFAULT_SERVICE_DESCRIPTOR))
                .build();
    }
}
