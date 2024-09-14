/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.wildfly.extension.clustering.server.registry;

import java.util.List;
import java.util.Map;
import java.util.function.BiFunction;
import java.util.function.Supplier;

import org.infinispan.Cache;
import org.wildfly.clustering.infinispan.service.InfinispanServiceDescriptor;
import org.wildfly.clustering.server.infinispan.CacheContainerGroup;
import org.wildfly.clustering.server.infinispan.CacheContainerGroupMember;
import org.wildfly.clustering.server.infinispan.registry.CacheRegistry;
import org.wildfly.clustering.server.infinispan.registry.CacheRegistryConfiguration;
import org.wildfly.clustering.server.registry.Registry;
import org.wildfly.clustering.server.registry.RegistryFactory;
import org.wildfly.clustering.server.service.BinaryServiceConfiguration;
import org.wildfly.clustering.server.service.ClusteringServiceDescriptor;
import org.wildfly.subsystem.service.ServiceDependency;
import org.wildfly.subsystem.service.ServiceInstaller;

/**
 * Configures a cache-based registry factory.
 * @author Paul Ferraro
 */
public class CacheRegistryFactoryServiceInstallerFactory<K, V> extends AbstractRegistryFactoryServiceInstallerFactory<K, V> {

    @Override
    public ServiceInstaller apply(BinaryServiceConfiguration configuration) {
        ServiceDependency<CacheContainerGroup> group = configuration.getServiceDependency(ClusteringServiceDescriptor.GROUP).map(CacheContainerGroup.class::cast);
        ServiceDependency<Cache<?, ?>> cache = configuration.getServiceDependency(InfinispanServiceDescriptor.CACHE);
        CacheRegistryConfiguration config = new CacheRegistryConfiguration() {
            @SuppressWarnings("unchecked")
            @Override
            public <KK, VV> Cache<KK, VV> getCache() {
                return (Cache<KK, VV>) cache.get();
            }

            @Override
            public CacheContainerGroup getGroup() {
                return group.get();
            }
        };
        Supplier<RegistryFactory<CacheContainerGroupMember, Object, Object>> factory = new Supplier<>() {
            @Override
            public RegistryFactory<CacheContainerGroupMember, Object, Object> get() {
                return RegistryFactory.singleton(new BiFunction<>() {
                    @Override
                    public Registry<CacheContainerGroupMember, Object, Object> apply(Map.Entry<Object, Object> entry, Runnable closeTask) {
                        return new CacheRegistry<>(config, entry, closeTask);
                    }
                });
            }
        };
        return ServiceInstaller.builder(factory).blocking()
                .provides(configuration.resolveServiceName(this.getServiceDescriptor()))
                .requires(List.of(group, cache))
                .build();
    }
}
