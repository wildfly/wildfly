/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.jboss.as.clustering.infinispan.subsystem;

import java.util.LinkedList;
import java.util.List;

import org.infinispan.configuration.global.TransportConfiguration;
import org.infinispan.configuration.global.TransportConfigurationBuilder;
import org.jboss.as.controller.OperationContext;
import org.jboss.as.controller.OperationFailedException;
import org.jboss.as.controller.PathAddress;
import org.jboss.as.controller.capability.RuntimeCapability;
import org.jboss.as.controller.capability.UnaryCapabilityNameResolver;
import org.jboss.dmr.ModelNode;
import org.wildfly.clustering.server.service.CacheContainerServiceInstallerProvider;
import org.wildfly.clustering.server.service.ClusteringServiceDescriptor;
import org.wildfly.clustering.server.service.ProvidedBiServiceInstallerProvider;
import org.wildfly.service.descriptor.UnaryServiceDescriptor;
import org.wildfly.subsystem.resource.ResourceDescriptor;
import org.wildfly.subsystem.resource.ResourceModelResolver;
import org.wildfly.subsystem.resource.operation.ResourceOperationRuntimeHandler;
import org.wildfly.subsystem.service.ResourceServiceConfigurator;
import org.wildfly.subsystem.service.ResourceServiceInstaller;

/**
 * Registers a resource definition for the transport of a cache container.
 * @author Paul Ferraro
 */
public abstract class TransportResourceDefinitionRegistrar extends ConfigurationResourceDefinitionRegistrar<TransportConfiguration, TransportConfigurationBuilder> implements ResourceServiceConfigurator {

    static final UnaryServiceDescriptor<TransportConfiguration> SERVICE_DESCRIPTOR = UnaryServiceDescriptorFactory.createServiceDescriptor(TransportResourceRegistration.WILDCARD, TransportConfiguration.class);
    static final RuntimeCapability<Void> CAPABILITY = RuntimeCapability.Builder.of(SERVICE_DESCRIPTOR).setDynamicNameMapper(UnaryCapabilityNameResolver.PARENT).build();

    static final RuntimeCapability<Void> COMMAND_DISPATCHER_FACTORY = RuntimeCapability.Builder.of(ClusteringServiceDescriptor.COMMAND_DISPATCHER_FACTORY).setDynamicNameMapper(UnaryCapabilityNameResolver.PARENT).setAllowMultipleRegistrations(true).build();
    static final RuntimeCapability<Void> GROUP = RuntimeCapability.Builder.of(ClusteringServiceDescriptor.GROUP).setDynamicNameMapper(UnaryCapabilityNameResolver.PARENT).setAllowMultipleRegistrations(true).build();

    interface Configurator extends ConfigurationResourceDefinitionRegistrar.Configurator<TransportConfiguration>, ResourceModelResolver<String> {
        @Override
        default RuntimeCapability<Void> getCapability() {
            return CAPABILITY;
        }
    }

    private final ResourceModelResolver<String> groupNameResolver;

    public TransportResourceDefinitionRegistrar(Configurator configurator) {
        super(configurator);
        this.groupNameResolver = configurator;
    }

    @Override
    public ResourceDescriptor.Builder apply(ResourceDescriptor.Builder builder) {
        return super.apply(builder).addCapabilities(List.of(COMMAND_DISPATCHER_FACTORY, GROUP));
    }

    @Override
    public ResourceOperationRuntimeHandler get() {
        return ResourceOperationRuntimeHandler.combine(super.get(), ResourceOperationRuntimeHandler.configureService(this));
    }

    @Override
    public ResourceServiceInstaller configure(OperationContext context, ModelNode model) throws OperationFailedException {
        PathAddress address = context.getCurrentAddress();
        PathAddress containerAddress = address.getParent();
        String name = containerAddress.getLastElement().getValue();

        String groupName = this.groupNameResolver.resolve(context, model);

        List<ResourceServiceInstaller> installers = new LinkedList<>();

        // Install services for CommandDispatcherFactory and Group
        new ProvidedBiServiceInstallerProvider<>(CacheContainerServiceInstallerProvider.class, CacheContainerServiceInstallerProvider.class.getClassLoader()).apply(name, groupName).forEach(installers::add);

        return ResourceServiceInstaller.combine(installers);
    }
}
