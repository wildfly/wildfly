/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.wildfly.clustering.server.service;

import org.wildfly.clustering.dispatcher.CommandDispatcherFactory;
import org.wildfly.clustering.group.Group;
import org.wildfly.clustering.provider.ServiceProviderRegistry;
import org.wildfly.clustering.registry.RegistryFactory;
import org.wildfly.service.descriptor.BinaryServiceDescriptor;
import org.wildfly.service.descriptor.NullaryServiceDescriptor;
import org.wildfly.service.descriptor.UnaryServiceDescriptor;

/**
 * Describes legacy clustering services.
 * @author Paul Ferraro
 */
@Deprecated
public interface LegacyClusteringServiceDescriptor {
    NullaryServiceDescriptor<CommandDispatcherFactory> DEFAULT_COMMAND_DISPATCHER_FACTORY = NullaryServiceDescriptor.of("org.wildfly.clustering.default-command-dispatcher-factory", CommandDispatcherFactory.class);
    NullaryServiceDescriptor<Group> DEFAULT_GROUP = NullaryServiceDescriptor.of("org.wildfly.clustering.default-group", Group.class);

    UnaryServiceDescriptor<CommandDispatcherFactory> COMMAND_DISPATCHER_FACTORY = UnaryServiceDescriptor.of("org.wildfly.clustering.command-dispatcher-factory", DEFAULT_COMMAND_DISPATCHER_FACTORY);
    UnaryServiceDescriptor<Group> GROUP = UnaryServiceDescriptor.of("org.wildfly.clustering.group", DEFAULT_GROUP);

    @SuppressWarnings("unchecked")
    UnaryServiceDescriptor<RegistryFactory<?, ?>> DEFAULT_REGISTRY_FACTORY = UnaryServiceDescriptor.of("org.wildfly.clustering.default-registry-factory", (Class<RegistryFactory<?, ?>>) (Class<?>) RegistryFactory.class);
    @SuppressWarnings("unchecked")
    UnaryServiceDescriptor<ServiceProviderRegistry<?>> DEFAULT_SERVICE_PROVIDER_REGISTRY = UnaryServiceDescriptor.of("org.wildfly.clustering.default-service-provider-registry", (Class<ServiceProviderRegistry<?>>) (Class<?>) ServiceProviderRegistry.class);

    BinaryServiceDescriptor<RegistryFactory<?, ?>> REGISTRY_FACTORY = BinaryServiceDescriptor.of("org.wildfly.clustering.registry-factory", DEFAULT_REGISTRY_FACTORY);
    BinaryServiceDescriptor<ServiceProviderRegistry<?>> SERVICE_PROVIDER_REGISTRY = BinaryServiceDescriptor.of("org.wildfly.clustering.service-provider-registry", DEFAULT_SERVICE_PROVIDER_REGISTRY);
}
