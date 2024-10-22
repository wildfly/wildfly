/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.wildfly.extension.clustering.server.registry.legacy;

import org.wildfly.clustering.server.local.LocalGroupMember;
import org.wildfly.clustering.server.registry.RegistryFactory;
import org.wildfly.clustering.server.service.BinaryServiceConfiguration;
import org.wildfly.clustering.server.service.ClusteringServiceDescriptor;
import org.wildfly.subsystem.service.ServiceDependency;
import org.wildfly.subsystem.service.ServiceInstaller;

/**
 * @author Paul Ferraro
 */
@Deprecated
public class LegacyLocalRegistryFactoryServiceInstallerFactory<K, V> extends LegacyRegistryFactoryServiceInstallerFactory<K, V> {

    @Override
    public ServiceInstaller apply(BinaryServiceConfiguration configuration) {
        ServiceDependency<RegistryFactory<LocalGroupMember, K, V>> factory = configuration.getServiceDependency(ClusteringServiceDescriptor.REGISTRY_FACTORY).map(RegistryFactory.class::cast);
        return ServiceInstaller.builder(LegacyLocalRegistryFactory::wrap, factory)
                .provides(configuration.resolveServiceName(this.getServiceDescriptor()))
                .requires(factory)
                .build();
    }
}
