/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.wildfly.clustering.infinispan.service;

import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;

import org.infinispan.commons.dataconversion.MediaType;
import org.infinispan.configuration.cache.CacheMode;
import org.infinispan.configuration.cache.Configuration;
import org.infinispan.configuration.cache.ConfigurationBuilder;
import org.infinispan.configuration.cache.StorageType;
import org.infinispan.manager.EmbeddedCacheManager;
import org.jboss.as.controller.RequirementServiceBuilder;
import org.jboss.msc.Service;
import org.wildfly.clustering.server.service.BinaryServiceConfiguration;
import org.wildfly.subsystem.service.ServiceDependency;
import org.wildfly.subsystem.service.ServiceInstaller;

/**
 * Configures a {@link Service} providing a cache {@link Configuration}.
 * @author Paul Ferraro
 */
public class ConfigurationServiceInstallerFactory implements Function<BinaryServiceConfiguration, ServiceInstaller> {

    private final Iterable<Consumer<RequirementServiceBuilder<?>>> dependencies;
    private final Consumer<ConfigurationBuilder> configurator;

    public ConfigurationServiceInstallerFactory(Consumer<ConfigurationBuilder> configurator, Iterable<Consumer<RequirementServiceBuilder<?>>> dependencies) {
        this.configurator = configurator;
        this.dependencies = dependencies;
    }

    @Override
    public ServiceInstaller apply(BinaryServiceConfiguration configuration) {
        ServiceDependency<EmbeddedCacheManager> container = configuration.getServiceDependency(InfinispanServiceDescriptor.CACHE_CONTAINER);
        Supplier<Configuration> factory = new Supplier<>() {
            @Override
            public Configuration get() {
                ConfigurationBuilder builder = new ConfigurationBuilder();
                ConfigurationServiceInstallerFactory.this.configurator.accept(builder);

                // Auto-enable simple cache optimization if cache is local, on-heap, non-transactional, and non-persistent, and statistics are disabled
                builder.simpleCache((builder.clustering().cacheMode() == CacheMode.LOCAL) && (builder.memory().storage() == StorageType.HEAP) && !builder.transaction().transactionMode().isTransactional() && builder.persistence().stores().isEmpty() && !builder.statistics().create().enabled());

                // Set media-type appropriate for the configured memory store
                builder.encoding().mediaType(builder.memory().storage().canStoreReferences() ? MediaType.APPLICATION_OBJECT_TYPE : container.get().getCacheManagerConfiguration().serialization().marshaller().mediaType().toString());
                return builder.build();
            }
        };
        String cacheName = configuration.getChildName();
        Consumer<Configuration> startTask = new Consumer<>() {
            @Override
            public void accept(Configuration configuration) {
                container.get().defineConfiguration(cacheName, configuration);
            }
        };
        Consumer<Configuration> stopTask = new Consumer<>() {
            @Override
            public void accept(Configuration configuration) {
                container.get().undefineConfiguration(cacheName);
            }
        };
        return ServiceInstaller.builder(factory)
                .provides(configuration.resolveServiceName(InfinispanServiceDescriptor.CACHE_CONFIGURATION))
                .requires(container)
                .requires(this.dependencies)
                .onStart(startTask)
                .onStop(stopTask)
                .build();
    }
}
