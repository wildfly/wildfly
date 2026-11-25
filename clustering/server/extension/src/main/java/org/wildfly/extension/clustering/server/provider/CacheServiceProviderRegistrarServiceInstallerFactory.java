/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.wildfly.extension.clustering.server.provider;

import java.util.List;

import org.infinispan.Cache;
import org.jboss.as.controller.management.Capabilities;
import org.jboss.msc.service.ServiceName;
import org.wildfly.clustering.function.Supplier;
import org.wildfly.clustering.infinispan.service.InfinispanServiceDescriptor;
import org.wildfly.clustering.server.infinispan.CacheContainerGroup;
import org.wildfly.clustering.server.infinispan.provider.CacheServiceProviderRegistrar;
import org.wildfly.clustering.server.service.BinaryServiceConfiguration;
import org.wildfly.clustering.server.service.ClusteringServiceDescriptor;
import org.wildfly.service.BlockingLifecycle;
import org.wildfly.subsystem.service.ServiceDependency;
import org.wildfly.subsystem.service.ServiceInstaller;

/**
 * Builds a cache-based {@link org.wildfly.clustering.server.provider.ServiceProviderRegistrar} service.
 * @author Paul Ferraro
 */
public class CacheServiceProviderRegistrarServiceInstallerFactory<T> extends ServiceProviderRegistrarServiceInstallerFactory<T> {

    @Override
    public ServiceInstaller apply(BinaryServiceConfiguration configuration) {
        ServiceDependency<CacheContainerGroup> group = configuration.getServiceDependency(ClusteringServiceDescriptor.GROUP).map(CacheContainerGroup.class::cast);
        ServiceDependency<Cache<?, ?>> cache = configuration.getServiceDependency(InfinispanServiceDescriptor.CACHE);
        ServiceName name = configuration.resolveServiceName(this.getServiceDescriptor());
        CacheServiceProviderRegistrar.Configuration config = new CacheServiceProviderRegistrar.Configuration() {
            @SuppressWarnings("unchecked")
            @Override
            public <K, V> Cache<K, V> getCache() {
                return (Cache<K, V>) cache.get();
            }

            @Override
            public CacheContainerGroup getGroup() {
                return group.get();
            }
        };
        return ServiceInstaller.BlockingBuilder.of(Supplier.of(config).thenApply(CacheServiceProviderRegistrar::new), ServiceDependency.on(Capabilities.MANAGEMENT_EXECUTOR))
                .provides(name)
                .requires(List.of(group, cache))
                .withLifecycle(BlockingLifecycle.autoClose())
                .build();
    }
}
