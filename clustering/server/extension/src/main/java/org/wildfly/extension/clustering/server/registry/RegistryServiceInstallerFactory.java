/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.wildfly.extension.clustering.server.registry;

import java.util.List;
import java.util.Map;
import java.util.function.Supplier;

import org.kohsuke.MetaInfServices;
import org.wildfly.clustering.server.GroupMember;
import org.wildfly.clustering.server.registry.Registry;
import org.wildfly.clustering.server.registry.RegistryFactory;
import org.wildfly.clustering.server.service.ClusteringServiceDescriptor;
import org.wildfly.common.function.Functions;
import org.wildfly.clustering.server.service.BinaryServiceConfiguration;
import org.wildfly.clustering.server.service.BinaryServiceInstallerFactory;
import org.wildfly.service.descriptor.BinaryServiceDescriptor;
import org.wildfly.subsystem.service.ServiceDependency;
import org.wildfly.subsystem.service.ServiceInstaller;

/**
 * @author Paul Ferraro
 */
@MetaInfServices(BinaryServiceInstallerFactory.class)
public class RegistryServiceInstallerFactory implements BinaryServiceInstallerFactory<Registry<GroupMember, Object, Object>> {

    @Override
    public ServiceInstaller apply(BinaryServiceConfiguration configuration) {
        ServiceDependency<RegistryFactory<GroupMember, Object, Object>> registryFactory = configuration.getServiceDependency(ClusteringServiceDescriptor.REGISTRY_FACTORY);
        ServiceDependency<Map.Entry<Object, Object>> registryEntry = configuration.getServiceDependency(ClusteringServiceDescriptor.REGISTRY_ENTRY);
        Supplier<Registry<GroupMember, Object, Object>> factory = new Supplier<>() {
            @Override
            public Registry<GroupMember, Object, Object> get() {
                return registryFactory.get().createRegistry(registryEntry.get());
            }
        };
        return ServiceInstaller.builder(factory)
                .onStop(Functions.closingConsumer())
                .provides(configuration.resolveServiceName(this.getServiceDescriptor()))
                .requires(List.of(registryFactory, registryEntry))
                .build();
    }

    @Override
    public BinaryServiceDescriptor<Registry<GroupMember, Object, Object>> getServiceDescriptor() {
        return ClusteringServiceDescriptor.REGISTRY;
    }
}
