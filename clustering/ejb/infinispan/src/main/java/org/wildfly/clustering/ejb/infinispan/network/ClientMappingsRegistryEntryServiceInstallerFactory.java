/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.wildfly.clustering.ejb.infinispan.network;

import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.function.Supplier;

import org.jboss.as.network.ClientMapping;
import org.wildfly.clustering.server.Group;
import org.wildfly.clustering.server.GroupMember;
import org.wildfly.clustering.server.service.BinaryServiceConfiguration;
import org.wildfly.clustering.server.service.ClusteringServiceDescriptor;
import org.wildfly.subsystem.service.ServiceDependency;
import org.wildfly.subsystem.service.ServiceInstaller;

/**
 * Configures a service that provides a client mappings registry entry for the local cluster member.
 * @author Paul Ferraro
 */
public class ClientMappingsRegistryEntryServiceInstallerFactory implements Function<BinaryServiceConfiguration, ServiceInstaller> {

    private final ServiceDependency<List<ClientMapping>> clientMappings;

    public ClientMappingsRegistryEntryServiceInstallerFactory(ServiceDependency<List<ClientMapping>> clientMappings) {
        this.clientMappings = clientMappings;
    }

    @Override
    public ServiceInstaller apply(BinaryServiceConfiguration configuration) {
        ServiceDependency<List<ClientMapping>> clientMappings = this.clientMappings;
        ServiceDependency<Group<GroupMember>> group = configuration.getServiceDependency(ClusteringServiceDescriptor.GROUP);
        Supplier<Map.Entry<String, List<ClientMapping>>> entry = new Supplier<>() {
            @Override
            public Map.Entry<String, List<ClientMapping>> get() {
                return new ClientMappingsRegistryEntry(group.get().getLocalMember().getName(), clientMappings.get());
            }
        };
        return ServiceInstaller.builder(entry)
                .provides(configuration.resolveServiceName(ClusteringServiceDescriptor.REGISTRY_ENTRY))
                .requires(List.of(group, this.clientMappings))
                .build();
    }
}
