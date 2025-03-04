/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.wildfly.extension.clustering.server.provider;

import java.util.List;
import java.util.function.Supplier;

import org.infinispan.Cache;
import org.jboss.as.controller.capability.CapabilityServiceSupport;
import org.jboss.msc.service.ServiceName;
import org.wildfly.clustering.infinispan.service.InfinispanServiceDescriptor;
import org.wildfly.clustering.server.infinispan.CacheContainerGroup;
import org.wildfly.clustering.server.infinispan.provider.CacheServiceProviderRegistrar;
import org.wildfly.clustering.server.infinispan.provider.CacheServiceProviderRegistrarConfiguration;
import org.wildfly.clustering.server.service.BinaryServiceConfiguration;
import org.wildfly.clustering.server.service.ClusteringServiceDescriptor;
import org.wildfly.common.function.Functions;
import org.wildfly.subsystem.service.ServiceDependency;
import org.wildfly.subsystem.service.ServiceInstaller;

/**
 * Builds a cache-based {@link ServiceProviderRegistrationFactory} service.
 * @author Paul Ferraro
 */
public class CacheServiceProviderRegistrarServiceInstallerFactory<T> extends AbstractServiceProviderRegistrarServiceInstallerFactory<T> {

    @Override
    public ServiceInstaller apply(CapabilityServiceSupport support, BinaryServiceConfiguration configuration) {
        ServiceDependency<CacheContainerGroup> group = configuration.getServiceDependency(ClusteringServiceDescriptor.GROUP).map(CacheContainerGroup.class::cast);
        ServiceDependency<Cache<?, ?>> cache = configuration.getServiceDependency(InfinispanServiceDescriptor.CACHE);
        ServiceName name = configuration.resolveServiceName(this.getServiceDescriptor());
        CacheServiceProviderRegistrarConfiguration config = new CacheServiceProviderRegistrarConfiguration() {
            @SuppressWarnings("unchecked")
            @Override
            public <K, V> Cache<K, V> getCache() {
                return (Cache<K, V>) cache.get();
            }

            @Override
            public Object getId() {
                return name;
            }

            @Override
            public CacheContainerGroup getGroup() {
                return group.get();
            }
        };
        Supplier<CacheServiceProviderRegistrar<Object>> factory = new Supplier<>() {
            @Override
            public CacheServiceProviderRegistrar<Object> get() {
                return new CacheServiceProviderRegistrar<>(config);
            }
        };
        return ServiceInstaller.builder(factory).blocking()
                .provides(name)
                .requires(List.of(group, cache))
                .onStop(Functions.closingConsumer())
                .build();
    }
}
