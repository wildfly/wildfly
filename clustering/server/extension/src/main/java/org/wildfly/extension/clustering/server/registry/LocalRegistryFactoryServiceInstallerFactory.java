/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.wildfly.extension.clustering.server.registry;

import java.util.Map;
import java.util.function.BiFunction;

import org.jboss.as.controller.descriptions.ModelDescriptionConstants;
import org.wildfly.clustering.function.Supplier;
import org.wildfly.clustering.server.local.LocalGroup;
import org.wildfly.clustering.server.local.LocalGroupMember;
import org.wildfly.clustering.server.local.registry.LocalRegistry;
import org.wildfly.clustering.server.registry.Registry;
import org.wildfly.clustering.server.registry.RegistryFactory;
import org.wildfly.clustering.server.service.BinaryServiceConfiguration;
import org.wildfly.clustering.server.service.ClusteringServiceDescriptor;
import org.wildfly.subsystem.service.ServiceDependency;
import org.wildfly.subsystem.service.ServiceInstaller;

/**
 * @author Paul Ferraro
 */
public class LocalRegistryFactoryServiceInstallerFactory<K, V> extends AbstractRegistryFactoryServiceInstallerFactory<K, V> {

    @Override
    public ServiceInstaller apply(BinaryServiceConfiguration configuration) {
        ServiceDependency<LocalGroup> group = ServiceDependency.on(ClusteringServiceDescriptor.GROUP, ModelDescriptionConstants.LOCAL).map(LocalGroup.class::cast);
        RegistryFactory<LocalGroupMember, K, V> factory = RegistryFactory.singleton(new BiFunction<>() {
            @Override
            public Registry<LocalGroupMember, K, V> apply(Map.Entry<K, V> entry, Runnable closeTask) {
                return LocalRegistry.of(group.get(), entry, closeTask);
            }
        });
        return ServiceInstaller.BlockingBuilder.of(Supplier.of(factory))
                .provides(configuration.resolveServiceName(this.getServiceDescriptor()))
                .requires(group)
                .build();
    }
}
