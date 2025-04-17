/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jboss.as.clustering.infinispan.subsystem;

import java.util.Collection;
import java.util.LinkedList;
import java.util.List;
import java.util.function.Consumer;

import org.jboss.as.clustering.infinispan.deployment.ClusteringDependencyProcessor;
import org.jboss.as.clustering.infinispan.logging.InfinispanLogger;
import org.jboss.as.clustering.infinispan.subsystem.remote.RemoteCacheContainerResourceDefinitionRegistrar;
import org.jboss.as.controller.OperationContext;
import org.jboss.as.controller.OperationFailedException;
import org.jboss.as.controller.ResourceDefinition;
import org.jboss.as.controller.SubsystemRegistration;
import org.jboss.as.controller.SubsystemResourceRegistration;
import org.jboss.as.controller.capability.RuntimeCapability;
import org.jboss.as.controller.capability.UnaryCapabilityNameResolver;
import org.jboss.as.controller.descriptions.ModelDescriptionConstants;
import org.jboss.as.controller.descriptions.ParentResourceDescriptionResolver;
import org.jboss.as.controller.descriptions.SubsystemResourceDescriptionResolver;
import org.jboss.as.controller.registry.ManagementResourceRegistration;
import org.jboss.as.controller.registry.RuntimePackageDependency;
import org.jboss.as.server.DeploymentProcessorTarget;
import org.jboss.as.server.deployment.Phase;
import org.jboss.dmr.ModelNode;
import org.wildfly.clustering.jgroups.spi.ChannelFactory;
import org.wildfly.clustering.server.service.ClusteringServiceDescriptor;
import org.wildfly.clustering.server.service.DefaultChannelServiceInstallerProvider;
import org.wildfly.clustering.server.service.LocalServiceInstallerProvider;
import org.wildfly.clustering.server.service.ProvidedUnaryServiceInstallerProvider;
import org.wildfly.subsystem.resource.ManagementResourceRegistrar;
import org.wildfly.subsystem.resource.ManagementResourceRegistrationContext;
import org.wildfly.subsystem.resource.ResourceDescriptor;
import org.wildfly.subsystem.resource.SubsystemResourceDefinitionRegistrar;
import org.wildfly.subsystem.resource.operation.ResourceOperationRuntimeHandler;
import org.wildfly.subsystem.service.ResourceServiceConfigurator;
import org.wildfly.subsystem.service.ResourceServiceInstaller;

/**
 * Registers a resource definition for the Infinispan subsystem.
 *
 * @author Richard Achmatowicz (c) 2011 Red Hat Inc.
 * @author Paul Ferraro
 */
public class InfinispanSubsystemResourceDefinitionRegistrar implements SubsystemResourceDefinitionRegistrar, Consumer<DeploymentProcessorTarget>, ResourceServiceConfigurator {
    static final SubsystemResourceRegistration REGISTRATION = SubsystemResourceRegistration.of("infinispan");
    public static final ParentResourceDescriptionResolver RESOLVER = new SubsystemResourceDescriptionResolver(REGISTRATION.getName(), InfinispanSubsystemResourceDefinitionRegistrar.class);

    // Registered as explicit requirement, until a proper subsystem is available
    static final String CLUSTERING_EXTENSION_MODULE = "org.wildfly.extension.clustering.server";

    @Override
    public ManagementResourceRegistration register(SubsystemRegistration parent, ManagementResourceRegistrationContext context) {

        RuntimeCapability<Void> defaultCommandDispatcherFactory = RuntimeCapability.Builder.of(ClusteringServiceDescriptor.DEFAULT_COMMAND_DISPATCHER_FACTORY).setAllowMultipleRegistrations(true).build();
        RuntimeCapability<Void> defaultGroup = RuntimeCapability.Builder.of(ClusteringServiceDescriptor.DEFAULT_GROUP).setAllowMultipleRegistrations(true).build();
        RuntimeCapability<Void> localCommandDispatcherFactory = RuntimeCapability.Builder.of(ClusteringServiceDescriptor.COMMAND_DISPATCHER_FACTORY).setDynamicNameMapper(UnaryCapabilityNameResolver.LOCAL).build();
        RuntimeCapability<Void> localGroup = RuntimeCapability.Builder.of(ClusteringServiceDescriptor.GROUP).setDynamicNameMapper(UnaryCapabilityNameResolver.LOCAL).build();

        ResourceDescriptor descriptor = ResourceDescriptor.builder(RESOLVER)
                .addCapabilities(List.of(defaultCommandDispatcherFactory, defaultGroup, localCommandDispatcherFactory, localGroup))
                .withDeploymentChainContributor(this)
                .withRuntimeHandler(ResourceOperationRuntimeHandler.configureService(this))
                .build();

        ManagementResourceRegistration registration = parent.registerSubsystemModel(ResourceDefinition.builder(REGISTRATION, RESOLVER).build());
        registration.registerAdditionalRuntimePackages(RuntimePackageDependency.required(CLUSTERING_EXTENSION_MODULE));

        ManagementResourceRegistrar.of(descriptor).register(registration);

        new CacheContainerResourceDefinitionRegistrar().register(registration, context);
        new RemoteCacheContainerResourceDefinitionRegistrar().register(registration, context);

        return registration;
    }

    @Override
    public void accept(DeploymentProcessorTarget target) {
        target.addDeploymentProcessor(REGISTRATION.getName(), Phase.DEPENDENCIES, Phase.DEPENDENCIES_CLUSTERING, new ClusteringDependencyProcessor());
    }

    @Override
    public ResourceServiceInstaller configure(OperationContext context, ModelNode model) throws OperationFailedException {
        InfinispanLogger.ROOT_LOGGER.activatingSubsystem();

        Collection<ResourceServiceInstaller> installers = new LinkedList<>();

        // Install local group services
        new ProvidedUnaryServiceInstallerProvider<>(LocalServiceInstallerProvider.class, LocalServiceInstallerProvider.class.getClassLoader()).apply(ModelDescriptionConstants.LOCAL).forEach(installers::add);

        // If JGroups subsystem is not available, install default group aliases to local group.
        if (!context.getCapabilityServiceSupport().hasCapability(ChannelFactory.DEFAULT_SERVICE_DESCRIPTOR)) {
            new ProvidedUnaryServiceInstallerProvider<>(DefaultChannelServiceInstallerProvider.class, DefaultChannelServiceInstallerProvider.class.getClassLoader()).apply(ModelDescriptionConstants.LOCAL).forEach(installers::add);
        }

        return ResourceServiceInstaller.combine(installers);
    }
}
