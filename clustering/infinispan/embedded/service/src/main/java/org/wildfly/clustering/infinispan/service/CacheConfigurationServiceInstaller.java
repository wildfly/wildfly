/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.wildfly.clustering.infinispan.service;

import java.util.List;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;

import org.infinispan.commons.dataconversion.MediaType;
import org.infinispan.configuration.cache.CacheMode;
import org.infinispan.configuration.cache.ConfigurationBuilder;
import org.infinispan.configuration.cache.StorageType;
import org.infinispan.manager.EmbeddedCacheManager;
import org.jboss.as.controller.RequirementServiceTarget;
import org.jboss.msc.service.ServiceController;
import org.wildfly.clustering.server.service.BinaryServiceConfiguration;
import org.wildfly.subsystem.service.ServiceDependency;
import org.wildfly.subsystem.service.ServiceInstaller;

/**
 * Installs an MSC service providing a cache configuration.
 * @author Paul Ferraro
 */
public class CacheConfigurationServiceInstaller implements ServiceInstaller {
    private static final Function<org.infinispan.configuration.cache.Configuration, ConfigurationBuilder> CLONE = new Function<>() {
        @Override
        public ConfigurationBuilder apply(org.infinispan.configuration.cache.Configuration configuration) {
            return new ConfigurationBuilder().read(configuration);
        }
    };

    public static ServiceDependency<ConfigurationBuilder> fromTemplate(BinaryServiceConfiguration configuration) {
        return configuration.getServiceDependency(InfinispanServiceDescriptor.CACHE_CONFIGURATION).map(CLONE);
    }

    private final BinaryServiceConfiguration configuration;
    private final ServiceDependency<ConfigurationBuilder> builder;

    public CacheConfigurationServiceInstaller(BinaryServiceConfiguration configuration, ServiceDependency<ConfigurationBuilder> builder) {
        this.configuration = configuration;
        this.builder = builder;
    }

    @Override
    public ServiceController<?> install(RequirementServiceTarget target) {
        ServiceDependency<EmbeddedCacheManager> manager = this.configuration.getServiceDependency(InfinispanServiceDescriptor.CACHE_CONTAINER);
        String cacheName = this.configuration.getChildName();
        Consumer<org.infinispan.configuration.cache.Configuration> start = new Consumer<>() {
            @Override
            public void accept(org.infinispan.configuration.cache.Configuration configuration) {
                manager.get().defineConfiguration(cacheName, configuration);
            }
        };
        Consumer<org.infinispan.configuration.cache.Configuration> stop = new Consumer<>() {
            @Override
            public void accept(org.infinispan.configuration.cache.Configuration configuration) {
                manager.get().undefineConfiguration(cacheName);
            }
        };
        Supplier<org.infinispan.configuration.cache.Configuration> factory = this.builder.map(new Function<>() {
            @Override
            public org.infinispan.configuration.cache.Configuration apply(ConfigurationBuilder builder) {
                // Auto-enable simple cache optimization if cache is local, on-heap, non-transactional, and non-persistent, and statistics are disabled
                builder.simpleCache((builder.clustering().cacheMode() == CacheMode.LOCAL) && (builder.memory().storage() == StorageType.HEAP) && !builder.transaction().transactionMode().isTransactional() && builder.persistence().stores().isEmpty() && !builder.statistics().create().enabled());

                // Set media-type appropriate for the configured memory store
                builder.encoding().mediaType(builder.memory().storage().canStoreReferences() ? MediaType.APPLICATION_OBJECT : manager.get().getCacheManagerConfiguration().serialization().marshaller().mediaType());

                return builder.build();
            }
        });

        return ServiceInstaller.builder(factory)
                .provides(this.configuration.resolveServiceName(InfinispanServiceDescriptor.CACHE_CONFIGURATION))
                .requires(List.of(this.builder, manager))
                .onStart(start)
                .onStop(stop)
                .build()
                .install(target);
    }
}
