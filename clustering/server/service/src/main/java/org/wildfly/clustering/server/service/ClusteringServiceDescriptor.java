/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.wildfly.clustering.server.service;

import java.util.Map;

import org.wildfly.clustering.server.Group;
import org.wildfly.clustering.server.GroupMember;
import org.wildfly.clustering.server.dispatcher.CommandDispatcherFactory;
import org.wildfly.clustering.server.provider.ServiceProviderRegistrar;
import org.wildfly.clustering.server.registry.Registry;
import org.wildfly.clustering.server.registry.RegistryFactory;
import org.wildfly.service.descriptor.BinaryServiceDescriptor;
import org.wildfly.service.descriptor.NullaryServiceDescriptor;
import org.wildfly.service.descriptor.UnaryServiceDescriptor;

/**
 * Describes clustering services.
 * @author Paul Ferraro
 */
public interface ClusteringServiceDescriptor {
    @SuppressWarnings("unchecked")
    NullaryServiceDescriptor<CommandDispatcherFactory<GroupMember>> DEFAULT_COMMAND_DISPATCHER_FACTORY = NullaryServiceDescriptor.of("org.wildfly.clustering.server.default-command-dispatcher-factory", (Class<CommandDispatcherFactory<GroupMember>>) (Class<?>) CommandDispatcherFactory.class);
    @SuppressWarnings("unchecked")
    NullaryServiceDescriptor<Group<GroupMember>> DEFAULT_GROUP = NullaryServiceDescriptor.of("org.wildfly.clustering.server.default-group", (Class<Group<GroupMember>>) (Class<?>) Group.class);

    UnaryServiceDescriptor<CommandDispatcherFactory<GroupMember>> COMMAND_DISPATCHER_FACTORY = UnaryServiceDescriptor.of("org.wildfly.clustering.server.command-dispatcher-factory", DEFAULT_COMMAND_DISPATCHER_FACTORY);
    UnaryServiceDescriptor<Group<GroupMember>> GROUP = UnaryServiceDescriptor.of("org.wildfly.clustering.server.group", DEFAULT_GROUP);

    @SuppressWarnings("unchecked")
    UnaryServiceDescriptor<Registry<GroupMember, Object, Object>> DEFAULT_REGISTRY = UnaryServiceDescriptor.of("org.wildfly.clustering.server.default-registry", (Class<Registry<GroupMember, Object, Object>>) (Class<?>) Registry.class);
    @SuppressWarnings("unchecked")
    UnaryServiceDescriptor<Map.Entry<Object, Object>> DEFAULT_REGISTRY_ENTRY = UnaryServiceDescriptor.of("org.wildfly.clustering.server.default-registry-entry", (Class<Map.Entry<Object, Object>>) (Class<?>) Map.Entry.class);
    @SuppressWarnings("unchecked")
    UnaryServiceDescriptor<RegistryFactory<GroupMember, Object, Object>> DEFAULT_REGISTRY_FACTORY = UnaryServiceDescriptor.of("org.wildfly.clustering.server.default-registry-factory", (Class<RegistryFactory<GroupMember, Object, Object>>) (Class<?>) RegistryFactory.class);
    @SuppressWarnings("unchecked")
    UnaryServiceDescriptor<ServiceProviderRegistrar<Object, GroupMember>> DEFAULT_SERVICE_PROVIDER_REGISTRAR = UnaryServiceDescriptor.of("org.wildfly.clustering.server.default-service-provider-registrar", (Class<ServiceProviderRegistrar<Object, GroupMember>>) (Class<?>) ServiceProviderRegistrar.class);

    BinaryServiceDescriptor<Registry<GroupMember, Object, Object>> REGISTRY = BinaryServiceDescriptor.of("org.wildfly.clustering.server.registry", DEFAULT_REGISTRY);
    BinaryServiceDescriptor<Map.Entry<Object, Object>> REGISTRY_ENTRY = BinaryServiceDescriptor.of("org.wildfly.clustering.server.registry-entry", DEFAULT_REGISTRY_ENTRY);
    BinaryServiceDescriptor<RegistryFactory<GroupMember, Object, Object>> REGISTRY_FACTORY = BinaryServiceDescriptor.of("org.wildfly.clustering.server.registry-factory", DEFAULT_REGISTRY_FACTORY);
    BinaryServiceDescriptor<ServiceProviderRegistrar<Object, GroupMember>> SERVICE_PROVIDER_REGISTRAR = BinaryServiceDescriptor.of("org.wildfly.clustering.server.service-provider-registrar", DEFAULT_SERVICE_PROVIDER_REGISTRAR);
}
