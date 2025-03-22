/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.wildfly.extension.clustering.web;

import java.util.EnumSet;
import java.util.List;
import java.util.function.Consumer;

import org.jboss.as.controller.OperationContext;
import org.jboss.as.controller.OperationFailedException;
import org.jboss.as.controller.ResourceDefinition;
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
import org.wildfly.clustering.web.service.session.DistributableSessionManagementProvider;
import org.wildfly.clustering.web.service.user.DistributableUserManagementProvider;
import org.wildfly.extension.clustering.web.deployment.DistributableWebDeploymentDependencyProcessor;
import org.wildfly.extension.clustering.web.deployment.DistributableWebDeploymentParsingProcessor;
import org.wildfly.extension.clustering.web.deployment.DistributableWebDeploymentProcessor;
import org.wildfly.extension.clustering.web.deployment.DistributableWebDeploymentSchema;
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
 * Registers a resource definition for the distributable-web subsystem.
 * @author Paul Ferraro
 */
public class DistributableWebSubsystemResourceDefinitionRegistrar implements SubsystemResourceDefinitionRegistrar, ResourceServiceConfigurator, Consumer<DeploymentProcessorTarget> {

    static final SubsystemResourceRegistration REGISTRATION = SubsystemResourceRegistration.of("distributable-web");
    static final ParentResourceDescriptionResolver RESOLVER = new SubsystemResourceDescriptionResolver(REGISTRATION.getName(), DistributableWebSubsystemResourceDefinitionRegistrar.class);

    static final RuntimeCapability<Void> DEFAULT_SESSION_MANAGEMENT_PROVIDER = RuntimeCapability.Builder.of(DistributableSessionManagementProvider.DEFAULT_SERVICE_DESCRIPTOR).build();
    static final RuntimeCapability<Void> DEFAULT_USER_MANAGEMENT_PROVIDER = RuntimeCapability.Builder.of(DistributableUserManagementProvider.DEFAULT_SERVICE_DESCRIPTOR).build();

    static final CapabilityReferenceAttributeDefinition<DistributableSessionManagementProvider> DEFAULT_SESSION_MANAGEMENT = new CapabilityReferenceAttributeDefinition.Builder<>("default-session-management", CapabilityReference.builder(DEFAULT_SESSION_MANAGEMENT_PROVIDER, DistributableSessionManagementProvider.SERVICE_DESCRIPTOR).build()).setXmlName(ModelDescriptionConstants.DEFAULT).build();
    static final CapabilityReferenceAttributeDefinition<DistributableUserManagementProvider> DEFAULT_USER_MANAGEMENT = new CapabilityReferenceAttributeDefinition.Builder<>("default-single-sign-on-management", CapabilityReference.builder(DEFAULT_USER_MANAGEMENT_PROVIDER, DistributableUserManagementProvider.SERVICE_DESCRIPTOR).build()).setXmlName(ModelDescriptionConstants.DEFAULT).build();

    @Override
    public ManagementResourceRegistration register(SubsystemRegistration parent, ManagementResourceRegistrationContext context) {
        ManagementResourceRegistration registration = parent.registerSubsystemModel(ResourceDefinition.builder(REGISTRATION, RESOLVER).build());
        ResourceDescriptor descriptor = ResourceDescriptor.builder(RESOLVER)
                .addAttributes(List.of(DEFAULT_SESSION_MANAGEMENT, DEFAULT_USER_MANAGEMENT))
                .addCapabilities(List.of(DEFAULT_SESSION_MANAGEMENT_PROVIDER, DEFAULT_USER_MANAGEMENT_PROVIDER))
                .requireSingletonChildResource(RoutingProviderResourceRegistration.LOCAL)
                .withRuntimeHandler(ResourceOperationRuntimeHandler.configureService(this))
                .withDeploymentChainContributor(this)
                .build();

        ManagementResourceRegistrar.of(descriptor).register(registration);

        new InfinispanSessionManagementResourceDefinitionRegistrar().register(registration, context);
        new HotRodSessionManagementResourceDefinitionRegistrar().register(registration, context);

        new InfinispanUserManagementResourceDefinitionRegistrar().register(registration, context);
        new HotRodUserManagementResourceDefinitionRegistrar().register(registration, context);

        new LocalRoutingProviderResourceDefinitionRegistrar().register(registration, context);
        new InfinispanRoutingProviderResourceDefinitionRegistrar().register(registration, context);

        return registration;
    }

    @Override
    public ResourceServiceInstaller configure(OperationContext context, ModelNode model) throws OperationFailedException {
        CapabilityServiceInstaller defaultSessionManagementInstaller = CapabilityServiceInstaller.builder(DEFAULT_SESSION_MANAGEMENT_PROVIDER, DEFAULT_SESSION_MANAGEMENT.resolve(context, model)).build();
        CapabilityServiceInstaller defaultUserManagementInstaller = CapabilityServiceInstaller.builder(DEFAULT_USER_MANAGEMENT_PROVIDER, DEFAULT_USER_MANAGEMENT.resolve(context, model)).build();

        return ResourceServiceInstaller.combine(defaultSessionManagementInstaller, defaultUserManagementInstaller);
    }

    @Override
    public void accept(DeploymentProcessorTarget target) {
        target.addDeploymentProcessor(REGISTRATION.getName(), Phase.STRUCTURE, Phase.STRUCTURE_REGISTER_JBOSS_ALL_DISTRIBUTABLE_WEB, JBossAllSchema.createDeploymentUnitProcessor(EnumSet.allOf(DistributableWebDeploymentSchema.class), DistributableWebDeploymentDependencyProcessor.CONFIGURATION_KEY));
        target.addDeploymentProcessor(REGISTRATION.getName(), Phase.PARSE, Phase.PARSE_DISTRIBUTABLE_WEB, new DistributableWebDeploymentParsingProcessor());
        target.addDeploymentProcessor(REGISTRATION.getName(), Phase.DEPENDENCIES, Phase.DEPENDENCIES_DISTRIBUTABLE_WEB, new DistributableWebDeploymentDependencyProcessor());
        target.addDeploymentProcessor(REGISTRATION.getName(), Phase.CONFIGURE_MODULE, Phase.CONFIGURE_DISTRIBUTABLE_WEB, new DistributableWebDeploymentProcessor());
    }
}
