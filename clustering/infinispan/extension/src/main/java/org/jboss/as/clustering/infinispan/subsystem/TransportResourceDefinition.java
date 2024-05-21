/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jboss.as.clustering.infinispan.subsystem;

import java.util.List;
import java.util.function.UnaryOperator;

import org.infinispan.configuration.global.TransportConfiguration;
import org.jboss.as.clustering.controller.ChildResourceDefinition;
import org.jboss.as.clustering.controller.ResourceDescriptor;
import org.jboss.as.clustering.controller.ResourceServiceHandler;
import org.jboss.as.clustering.controller.SimpleResourceRegistrar;
import org.jboss.as.controller.PathElement;
import org.jboss.as.controller.capability.RuntimeCapability;
import org.jboss.as.controller.capability.UnaryCapabilityNameResolver;
import org.jboss.as.controller.registry.ManagementResourceRegistration;
import org.wildfly.clustering.infinispan.service.InfinispanServiceDescriptor;
import org.wildfly.clustering.server.service.ClusteringServiceDescriptor;
import org.wildfly.service.descriptor.UnaryServiceDescriptor;
import org.wildfly.subsystem.resource.operation.ResourceOperationRuntimeHandler;
import org.wildfly.subsystem.service.ResourceServiceConfigurator;

/**
 * @author Paul Ferraro
 */
public abstract class TransportResourceDefinition extends ChildResourceDefinition<ManagementResourceRegistration> implements ResourceServiceConfigurator {

    static final PathElement WILDCARD_PATH = pathElement(PathElement.WILDCARD_VALUE);
    static PathElement pathElement(String value) {
        return PathElement.pathElement("transport", value);
    }

    static final UnaryServiceDescriptor<TransportConfiguration> SERVICE_DESCRIPTOR = UnaryServiceDescriptor.of(InfinispanServiceDescriptor.CACHE_CONTAINER_CONFIGURATION.getName() + "." + WILDCARD_PATH.getKey(), TransportConfiguration.class);
    static final RuntimeCapability<Void> CAPABILITY = createRuntimeCapability(SERVICE_DESCRIPTOR);

    private static RuntimeCapability<Void> createRuntimeCapability(UnaryServiceDescriptor<?> descriptor) {
        return RuntimeCapability.Builder.of(descriptor).setAllowMultipleRegistrations(true).setDynamicNameMapper(UnaryCapabilityNameResolver.PARENT).build();
    }

    private final UnaryOperator<ResourceDescriptor> configurator;

    TransportResourceDefinition(PathElement path, UnaryOperator<ResourceDescriptor> configurator) {
        super(path, InfinispanExtension.SUBSYSTEM_RESOLVER.createChildResolver(path));
        this.configurator = configurator;
    }

    @Override
    public ManagementResourceRegistration register(ManagementResourceRegistration parent) {
        ManagementResourceRegistration registration = parent.registerSubModel(this);

        RuntimeCapability<Void> commandDispatcherFactory = RuntimeCapability.Builder.of(ClusteringServiceDescriptor.COMMAND_DISPATCHER_FACTORY).setDynamicNameMapper(UnaryCapabilityNameResolver.PARENT).setAllowMultipleRegistrations(true).build();
        RuntimeCapability<Void> group = RuntimeCapability.Builder.of(ClusteringServiceDescriptor.GROUP).setDynamicNameMapper(UnaryCapabilityNameResolver.PARENT).setAllowMultipleRegistrations(true).build();

        ResourceDescriptor descriptor = this.configurator.apply(new ResourceDescriptor(this.getResourceDescriptionResolver()))
                .addCapabilities(List.of(CAPABILITY, commandDispatcherFactory, group))
                ;
        ResourceOperationRuntimeHandler handler = ResourceOperationRuntimeHandler.configureService(this);
        new SimpleResourceRegistrar(descriptor, ResourceServiceHandler.of(handler)).register(registration);

        return registration;
    }
}
