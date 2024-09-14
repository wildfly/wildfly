/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jboss.as.clustering.infinispan.subsystem;

import java.util.Collection;
import java.util.LinkedList;
import java.util.List;
import java.util.function.Consumer;

import org.jboss.as.clustering.controller.DeploymentChainContributingResourceRegistrar;
import org.jboss.as.clustering.controller.ManagementResourceRegistration;
import org.jboss.as.clustering.controller.ResourceDescriptor;
import org.jboss.as.clustering.controller.ResourceServiceHandler;
import org.jboss.as.clustering.controller.SubsystemRegistration;
import org.jboss.as.clustering.controller.SubsystemResourceDefinition;
import org.jboss.as.clustering.infinispan.deployment.ClusteringDependencyProcessor;
import org.jboss.as.clustering.infinispan.logging.InfinispanLogger;
import org.jboss.as.clustering.infinispan.subsystem.remote.RemoteCacheContainerResourceDefinition;
import org.jboss.as.controller.OperationContext;
import org.jboss.as.controller.OperationFailedException;
import org.jboss.as.controller.PathElement;
import org.jboss.as.controller.capability.RuntimeCapability;
import org.jboss.as.controller.capability.UnaryCapabilityNameResolver;
import org.jboss.as.controller.descriptions.ModelDescriptionConstants;
import org.jboss.as.controller.operations.common.GenericSubsystemDescribeHandler;
import org.jboss.as.controller.registry.RuntimePackageDependency;
import org.jboss.as.server.DeploymentProcessorTarget;
import org.jboss.as.server.deployment.Phase;
import org.jboss.dmr.ModelNode;
import org.wildfly.clustering.jgroups.spi.ChannelFactory;
import org.wildfly.clustering.server.service.ClusteringServiceDescriptor;
import org.wildfly.clustering.server.service.DefaultChannelServiceInstallerProvider;
import org.wildfly.clustering.server.service.LocalServiceInstallerProvider;
import org.wildfly.clustering.server.service.ProvidedUnaryServiceInstallerProvider;
import org.wildfly.subsystem.resource.operation.ResourceOperationRuntimeHandler;
import org.wildfly.subsystem.service.ResourceServiceConfigurator;
import org.wildfly.subsystem.service.ResourceServiceInstaller;

/**
 * The root resource of the Infinispan subsystem.
 *
 * @author Richard Achmatowicz (c) 2011 Red Hat Inc.
 */
public class InfinispanSubsystemResourceDefinition extends SubsystemResourceDefinition implements Consumer<DeploymentProcessorTarget>, ResourceServiceConfigurator {

    static final PathElement PATH = pathElement(InfinispanExtension.SUBSYSTEM_NAME);
    // Registered as explicit requirement, until a proper subsystem is available
    static final String CLUSTERING_EXTENSION_MODULE = "org.wildfly.extension.clustering.server";

    InfinispanSubsystemResourceDefinition() {
        super(PATH, InfinispanExtension.SUBSYSTEM_RESOLVER);
    }

    @Override
    public void register(SubsystemRegistration parentRegistration) {
        ManagementResourceRegistration registration = parentRegistration.registerSubsystemModel(this);
        registration.registerAdditionalRuntimePackages(RuntimePackageDependency.required(CLUSTERING_EXTENSION_MODULE));
        registration.registerOperationHandler(GenericSubsystemDescribeHandler.DEFINITION, GenericSubsystemDescribeHandler.INSTANCE);

        RuntimeCapability<Void> defaultCommandDispatcherFactory = RuntimeCapability.Builder.of(ClusteringServiceDescriptor.DEFAULT_COMMAND_DISPATCHER_FACTORY).setAllowMultipleRegistrations(true).build();
        RuntimeCapability<Void> defaultGroup = RuntimeCapability.Builder.of(ClusteringServiceDescriptor.DEFAULT_GROUP).setAllowMultipleRegistrations(true).build();

        RuntimeCapability<Void> localCommandDispatcherFactory = RuntimeCapability.Builder.of(ClusteringServiceDescriptor.COMMAND_DISPATCHER_FACTORY).setDynamicNameMapper(UnaryCapabilityNameResolver.LOCAL).build();
        RuntimeCapability<Void> localGroup = RuntimeCapability.Builder.of(ClusteringServiceDescriptor.GROUP).setDynamicNameMapper(UnaryCapabilityNameResolver.LOCAL).build();

        ResourceDescriptor descriptor = new ResourceDescriptor(this.getResourceDescriptionResolver())
                .addCapabilities(List.of(defaultCommandDispatcherFactory, defaultGroup, localCommandDispatcherFactory, localGroup))
                ;
        ResourceOperationRuntimeHandler handler = ResourceOperationRuntimeHandler.configureService(this);
        new DeploymentChainContributingResourceRegistrar(descriptor, ResourceServiceHandler.of(handler), this).register(registration);

        new CacheContainerResourceDefinition().register(registration);
        new RemoteCacheContainerResourceDefinition().register(registration);
    }

    @Override
    public void accept(DeploymentProcessorTarget target) {
        target.addDeploymentProcessor(InfinispanExtension.SUBSYSTEM_NAME, Phase.DEPENDENCIES, Phase.DEPENDENCIES_CLUSTERING, new ClusteringDependencyProcessor());
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
